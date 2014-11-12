# -*- coding: utf-8 -*-

"""
snailbot
=====================
The bot to manage Snailbucket tourneys on Free Internet Chess Server
based on mekk.fics library.

"""
from __future__ import print_function

import getopt, sys, logging, os
from twisted.internet import defer, reactor, task
from twisted.enterprise import adbapi

from mekk.fics import ReconnectingFicsFactory, FicsClient, FicsEventMethodsMixin
from mekk.fics import TellCommandsMixin, TellCommand

logger = logging.getLogger("snail")

#################################################################################
# Configuration
#################################################################################

from mekk.fics import FICS_HOST, FICS_PORT

FICS_USER='snailbotguest'
FICS_PASSWORD=''

FINGER_TEXT = """Join Snail Bucket http://snailbucket.org/ FICS chess community for some loooong time controls.


This bot is run by Bodia
Usage:
    tell snailbot join
    tell snailbot play
"""

script_dir = os.path.dirname(os.path.abspath(__file__))

#################################################################################
# ”Business logic” (processing not directly bound to FICS interface)
#################################################################################

class SnailBot(object):
    def __init__(self, dbpool):
        self.dbpool = dbpool

    def __del__(self):
        self.dbpool.close()

    db_initialized = False

    def save_unregistered_player(self, who):     
        return self.dbpool.runQuery("INSERT INTO MEMBERS(CONFIRMED, GRUP, USERNAME) VALUES (0, 1, '"+who+"')")

    # TODO: move logic of "play" command here

#################################################################################
# Commands (handling tells to the bot)
#################################################################################

class JoinCommand(TellCommand):
    """
    Join snailbucket community
    """

    def __init__(self, clock_statistician):
        self.clock_statistician = clock_statistician
    @classmethod
    def named_parameters(cls):
        return {}
    @classmethod
    def positional_parameters_count(cls):       
        return 0, 0
    @defer.inlineCallbacks
    def run(self, fics_client, player, *args, **kwargs):
        yield self.clock_statistician.save_unregistered_player(player.name)
        yield fics_client.tell_to(player, "You are registered. Please use the following form to proceed: http://www.snailbucket.org/wiki/Special:Register")

    def help(self, fics_client):
        return "Initiates the join to SnailBucket"


class PlayCommand(TellCommand):
    """
    Play scheduled snailbucket game
    """

    def __init__(self, clock_statistician):
        self.clock_statistician = clock_statistician

    ##
    # http://snailbucket.org/wiki/Matching_time_controls_algorithm
    ##
    def recommend_time(self, white_preference, black_preference):
        stripped_white = [x.strip().replace("45 45", "45_45") for x in white_preference.split(",")]
        stripped_black = [x.strip().replace("45 45", "45_45") for x in black_preference.split(",")]

        def intersect(a, b):
            return list(set(a) & set(b))

        inters = intersect(stripped_white, stripped_black)

        best_value = 1000
        best_tc = "45_45"
        for tc in inters:
            of = stripped_white.index(tc) + stripped_black.index(tc)
            if of < best_value:
                best_value = of
                best_tc = tc
            elif of == best_value:
                if int(tc.replace("75_0", "75_00").replace("_", "")) < int(best_tc.replace("75_0", "75_00").replace("_", "")):
                    best_value = of
                    best_tc = tc

        return best_tc

    ##
    # Return the game parameters of caller's scheduled game if there is any
    ##    
    def get_game_data(self, caller):

        def stat(tx):
            r = tx.execute("select TOURN_PLAYERS.ID from TOURN_PLAYERS inner join MEMBERS on MEMBERS.ID = TOURN_PLAYERS.MEMBER_ID"
                           " where MEMBERS.username = '"+caller+"'")
            player_id = str(tx.fetchall()[0][0])
            tx.execute(
            "select ID from TOURN_GAMES where (BLACKPL_ID = "+player_id+" or WHITEPL_ID = "+player_id+") and SHEDULED_DATE IS NOT NULL "
                                                                                                      "and RESULT IS NULL"
            )
            game_id = str(tx.fetchall()[0][0])
            tx.execute("select MEMBERS.USERNAME, MEMBERS.PREFERENCE from MEMBERS inner join TOURN_PLAYERS on MEMBERS.ID = TOURN_PLAYERS.MEMBER_ID where "
                    "TOURN_PLAYERS.ID = (select WHITEPL_ID from TOURN_GAMES where ID="+game_id+")")
            white_username, white_preference = tx.fetchall()[0]
            tx.execute("select MEMBERS.USERNAME, MEMBERS.PREFERENCE from MEMBERS inner join TOURN_PLAYERS on MEMBERS.ID = TOURN_PLAYERS.MEMBER_ID where "
                   "TOURN_PLAYERS.ID = (select BLACKPL_ID from TOURN_GAMES where ID="+game_id+")")
            black_username, black_preference = tx.fetchall()[0]
            return (white_username, black_username, self.recommend_time(white_preference, black_preference).replace("_", " "))

        return dbpool.runInteraction(stat)


    @classmethod
    def named_parameters(cls):
        return {}
    @classmethod
    def positional_parameters_count(cls):
        return 0, 0
    @defer.inlineCallbacks
    def run(self, fics_client, player, *args, **kwargs):

        def process(res):
            if res[0] == player.name:
                result = fics_client.run_command("rmatch %s %s %s %s" % (res[0], res[1], res[2], "white"))
                fics_client.tell_to(player, "Match request has been sent")
            else:
                result = fics_client.run_command("rmatch %s %s %s %s" % (res[1], res[0], res[2], "black"))

            print (result)

        x = self.get_game_data(player.name)
        x.addCallback(process)
        yield x


    def help(self, fics_client):
        return "Start a snailbucket game"



class HelpCommand(TellCommand):
      """
   Help command: TODO: make it work
    """
  
    @classmethod
    def named_parameters(cls):
        return {}
    @classmethod
    def positional_parameters_count(cls):
        return 0,1

    def run(self, fics_client, player, *args, **kwargs):
        if args:
            return fics_client.command_help(args[0])
        else:
            return "I support the following commands: %s.\nFor more help try: %s" % (
                ", ".join(fics_client.command_names()),
                ", ".join(
                    "\"tell %s help %s\"" % (fics_client.fics_user_name, command)
                    for command in fics_client.command_names()
                    if command != "help"))
    def help(self, fics_client):
        return "I print some help"

#################################################################################
# The bot core
#################################################################################

class MyBot(
    TellCommandsMixin,
    FicsEventMethodsMixin,
    FicsClient
):

    def __init__(self, clock_statistician):
        FicsClient.__init__(self, label="clock-stats-bot")

        self.clock_statistician = clock_statistician

        self.use_keep_alive = True
        self.variables_to_set_after_login = {
            'shout': 0,
            'cshout': 0,
            'tzone': 'EURCST',
            'tell': 0,
            'noescape': 0,
            'kibitz': 0,
            # Enable guest tells
            'guest': 0,
            # Listen to games notifications
            'gin' : 0,
            }
        self.interface_variables_to_set_after_login = [
            # For rich info about game started
            ]

        self.register_command(JoinCommand(self.clock_statistician))
        self.register_command(PlayCommand(self.clock_statistician))
        self.register_command(HelpCommand())


    def on_login(self, my_username):
        print("I am logged as %s, use \"tell %s help\" to start conversation on FICS" % (
            my_username, my_username))
        
        # Normal post-login processing
        return defer.DeferredList([
                self.set_finger(FINGER_TEXT),
                # Commands below are unnecessary as variables_to_set_after_login above
                # defines them. Still, this form may be useful if we dynamically enable/disable
                # things.
                #  self.enable_seeks(),
                #  self.enable_guest_tells(),
                #  self.enable_games_tracking(),
                #  self.enable_users_tracking(),
                self.subscribe_channel(101),
                self.unsubscribe_channel(49), # TODO: tournament stats
                self.unsubscribe_channel(50),
                self.unsubscribe_channel(2),
                # self.subscribe_channel(90),
                self.run_command("+censor relay")
                ])

    def on_logout(self):
        if hasattr(self, '_finger_refresh_task'):
            self._finger_refresh_task.stop()
            del self._finger_refresh_task


#################################################################################
# Script argument processing
#################################################################################

# TODO: --silent with no logging except errors

options, remainders = getopt.getopt(args = sys.argv[1:], shortopts=[], longopts=["debug"])

if "--debug" in [name for name,_ in options]:
    logging_level = logging.DEBUG
else:
    #logging_level = logging.WARN
    logging_level = logging.INFO

logging.basicConfig(level=logging_level)

#################################################################################
# Startup glue code
#################################################################################

# TODO: convert back to reconnecting

dbpool = adbapi.ConnectionPool("MySQLdb", user="bodia", passwd="pass", db="test_db"
)

clock_statistician = SnailBot(dbpool)
my_bot = MyBot(clock_statistician)
reactor.connectTCP(
    FICS_HOST, FICS_PORT,
    ReconnectingFicsFactory(
        client=my_bot,
        auth_username=FICS_USER, auth_password=FICS_PASSWORD)
)
#noinspection PyUnresolvedReferences
reactor.run()



