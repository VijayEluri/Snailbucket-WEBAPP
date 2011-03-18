/**
 * No copyright. The code is released into the public domain according to
 * the definition of "public domain" in your country.  
 */

package net.rwchess.site.utils;

import info.bliki.wiki.model.WikiModel;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import chesspresso.Chess;
import chesspresso.game.*;
import chesspresso.move.Move;
import chesspresso.pgn.*;

import com.google.appengine.api.datastore.Text;

import net.rwchess.site.data.RWMember;
import net.rwchess.site.data.RWSwissPlayer;
import net.rwchess.site.data.SwissGuest;
import net.rwchess.site.data.T45Player;

/**
 * Some useful utility methods.
 * @author bodia
 *
 */
public final class UsefulMethods {
	
	private static final WikiModel wikiModel = new WikiModel(
			"/files/${image}",
			"/wiki/${title}");
	
	private static SimpleDateFormat dateFormat;

	private UsefulMethods() {} // provides non-instensability
	
	/**
	 * Gets a plain password string and returns MD5 hash
	 */
	public static String getMD5(String passwd) {
		MessageDigest alg = null;
		try {
			alg = MessageDigest.getInstance("MD5");
			alg.reset(); 		
			alg.update(passwd.getBytes());
		} 
		catch (NoSuchAlgorithmException e) {}			
		
		byte[] digest = alg.digest();
		
		StringBuffer hashedpasswd = new StringBuffer();
		String hx;
		for (int i=0;i<digest.length;i++){
			hx =  Integer.toHexString(0xFF & digest[i]);
			if(hx.length() == 1){hx = "0" + hx;} 
			hashedpasswd.append(hx);
		}

        return hashedpasswd.toString();
	}
	
	/**
	 * Inserts HTML breaklines into the text
	 */
	public static String parseNewsText(String txt) {
		StringBuilder bui = new StringBuilder();
		bui.append("<p>");
		for (char c: txt.toCharArray()) {
			if (c == '\n') 
				bui.append("</p><p>");
			else 
				bui.append(c);
		}
		bui.append("</p>");
		return bui.toString();
	}
	
	/**
	 * Returns current user username using session data
	 */
	public static String getUsername(HttpSession s) {
		if (s.getAttribute("user") != null)
		     return ((UsernameComparable) s.getAttribute("user")).getUsername();
		else return "null";
	}
	
	/**
	 * Converts the number representation of a group to word
	 */
	public static String groupToWord(int group) {
		switch (group) {
		case RWMember.ADMIN:
			return "admin";
		case RWMember.MODERATOR:
			return "moderator";
		case RWMember.TD:
			return "td";
		default:
			return "member";
		}
	}

	/**
	 * Does the reverse process of groupToWord()
	 */
	public static int wordToGroup(String word) {
		if (word.equalsIgnoreCase("admin")) return RWMember.ADMIN;
		else if (word.equalsIgnoreCase("td")) return RWMember.TD;
		else if (word.equalsIgnoreCase("moderator")) return RWMember.MODERATOR;
		else return RWMember.MEMBER;
	}

	/**
	 * Creates footer to wrap error text
	 */
	public static void doDesignFooter(ServletRequest request,
			ServletResponse response) {
		try {
			// calls bottom jsp
			request.getRequestDispatcher("/blocks/bottom.jsp").include(request,
					response);
		} 
		catch (ServletException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates header to wrap error text
	 */
	public static void doDesignHeader(ServletRequest request,
			ServletResponse response) {
		try {
			request.getRequestDispatcher("/blocks/top.jsp").include(request,
					response);
			request.getRequestDispatcher("/blocks/currevents.jsp").include(
					request, response);
		} 
		catch (ServletException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public static Text getHtml(Text rawText) {
		return new Text(wikiModel.render(rawText.getValue()));
	}

	/**
	 * @param content Result in string representation
	 * @param isRwFirst count points for first or second team
	 * @return number of points what RW members have won
	 */
	public static double parseStringToPoints(String content, boolean isRwFirst) {
		if (content.equals("1/2-1/2"))
			return 0.5;
		else if (((content.equals("1-0") || content.equals("i-o")) && isRwFirst)
				|| ((content.equals("0-1") || content.equals("o-i"))
				&& !isRwFirst))
			return 1;
		return 0;
	}

	public static boolean lookLikeXss(String uname) {
		for (char c: uname.toCharArray()) {
			if (c == '<' || c == '>' || c == ' ' || c == '/')
				return true;
		}
		return false;
	}	
	
	public static String capitalize(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0)
			return str;

		return new StringBuffer(strLen).append(
				Character.toTitleCase(str.charAt(0))).append(str.substring(1))
				.toString();
	}

	public static SimpleDateFormat getWikiDateFormatter() {
		if (dateFormat == null)
			dateFormat = new SimpleDateFormat("h:mm, d MMMMM yyyy", Locale.US);
		
		return dateFormat;
	}
	
	public static byte[] concat(byte[] A, byte[] B) {
		   byte[] C= new byte[A.length+B.length];
		   System.arraycopy(A, 0, C, 0, A.length);
		   System.arraycopy(B, 0, C, A.length, B.length);

		   return C;
		}

	
	public static String getMembersTableHtml(List<RWMember> members, List<String> aliveUsers) {
		StringBuffer buff = new StringBuffer();
		//int maxRows = members.size()/4;
		buff.append("<table border=\"0\" align=\"center\">");
		int coloumn = 0;

		for (RWMember m : members) {
			if (coloumn == 0)
				buff.append("<tr>");
			else if (coloumn == 3) {
				buff.append("</tr>");
				coloumn = 0;
			}
			
			buff.append("<td width=\"25%\">");
			buff.append("<img src=\"http://simile.mit.edu/exhibit/examples/flags/images/" +
					""+m.getCountry()+".png\" border=\"0\"/>");
			
			if (containAlive(aliveUsers, m.getUsername())) {
				buff.append("<a href=\"/wiki/User:"
						+ m.getUsername() + "\" style=\"color: #DC143C\">" + m.getUsername()
						+ "</a>");
			} else
				buff.append("<a href=\"/wiki/User:" + m.getUsername() + "\">"
						+ m.getUsername() + "</a>");

			buff.append("</td>");
			coloumn++;
		}
		buff.append("</table>");
		return buff.toString();
	}
	
	private static boolean containAlive(List<String> aliveUsers, String username) {
		for (String u: aliveUsers) {
			if (u.equals(username))
				return true;
		}
		return false;
	}

	/**
	 * A dirty hack that retrieves the actually requested URI from 
	 * an HTTP request
	 * @param request Text of HTTP request
	 * @return URI
	 */
	public static String getRealQueryURI(String request) {
		boolean start = false;
		boolean sign = false;
		StringBuffer buf = new StringBuffer();
		for (char c: request.toCharArray()) {
			if (c == '?' && sign)
				break;
				
			if (c == '?')
				sign = true;
			
			if (c == ' ' && start) 
				break;
			
			if (c == ' ') {
				start = true;
				continue;
			}
			
			if (c == '\n')
				break;
			
			if (start)
				buf.append(c);
		}
		return buf.toString();
	}
	
	public static String avlbByteToString(byte a) {
		switch(a) {
		case 0:
			return "All time";
		case 1:
			return "Most time";
		case 2:
			return "Reserve";
		case 3:
			return "Unavailable";	
		}
		return "";
	}

	public static String getTlParticipantsHtml(List<T45Player> allPlayers) {
		StringBuffer buff = new StringBuffer();		
		for (T45Player pl : allPlayers) {
			buff.append("<tr>");
			buff.append("<td>" + pl.getUsername() + "</td>");
			buff.append("<td>" + pl.getFixedRating() + "</td>");
			buff.append("<td>" + pl.getPreferedSection() + "</td>");
			buff.append("<td>" + UsefulMethods.avlbByteToString(pl.getAvailability()) + "</td>");
			buff.append("</tr>");
		}
		return buff.toString();
	}
	
	public static UsernameComparable findForUname(String uname, 
			List<UsernameComparable> list) {
		for (UsernameComparable uc: list) {
			if (uc.equals(uname))
				return uc;
		}
		return null;
	}
	
	public static String getSwissParticipantsHtml(List<RWSwissPlayer> allPlayers, 
			List<RWMember> allMembers, List<SwissGuest> allGuests) {
		StringBuffer buff = new StringBuffer();
		int i = 0;
		int size = allPlayers.size();
		if (size%2 == 0)
			size = size/2;
		else
			size = (size-1)/2 + 1;
		
		while (size > i) {
			RWSwissPlayer pl1 = allPlayers.get(i);
			RWSwissPlayer pl2;
			
			if (allPlayers.size() == (i+size)) {
				pl2 = new RWSwissPlayer();
				pl2.setFixedRating(0);
				pl2.setUsername("");
			}
			else
				pl2 = allPlayers.get(i+size);
			
			String country1 = null;
			boolean guest1 = false;
			for (RWMember uc: allMembers) {
				if (uc.getUsername().equalsIgnoreCase(pl1.getUsername()))
					country1 = uc.getCountry();
			}
			if (country1 == null) {
				for (SwissGuest uc: allGuests) {
					if (uc.getUsername().equalsIgnoreCase(pl1.getUsername())) {
						country1 = uc.getCountry();
						guest1 = true;
					}
				}
			}
			
			
			String country2 = "";
			boolean guest2 = false;
			if (!pl2.getUsername().isEmpty()) {
				for (RWMember uc : allMembers) {
					if (uc.getUsername().equalsIgnoreCase(pl2.getUsername()))
						country2 = uc.getCountry();
				}
				if (country2.isEmpty()) {
					for (SwissGuest uc : allGuests) {
						if (uc.getUsername().equalsIgnoreCase(pl2.getUsername())) {
							country2 = uc.getCountry();
							guest2 = true;
						}
					}
				}
			}
			
			
			buff.append("<tr>");
			buff.append("<td align=\"right\">"+(i+1)+"</td>");
			buff.append("<td align=\"left\">");
			if (country1 != null)
				buff.append("<img src=\"http://simile.mit.edu/exhibit/examples/flags/images/" +
						""+country1.toLowerCase()+".png\" border=\"0\"/>");
			buff.append(pl1.getUsername() + (guest1 ? "(G)" : ""));
			buff.append("</td>");
			buff.append("<td align=\"center\">" + pl1.getFixedRating() + "</td>");
			
			if (!pl2.getUsername().isEmpty()) {
				buff.append("<td align=\"right\">" + (i + size + 1) + "</td>");
				buff.append("<td align=\"left\">");
				if (!country2.isEmpty()) {
					buff
							.append("<img src=\"http://simile.mit.edu/exhibit/examples/flags/images/"
									+ ""
									+ country2.toLowerCase()
									+ ".png\" border=\"0\"/>");
				}
				buff.append(pl2.getUsername() + (guest2 ? "(G)" : ""));
				buff.append("</td>");
				buff.append("<td align=\"center\">"
						+ (pl2.getFixedRating() == 0 ? "" : pl2
								.getFixedRating()) + "</td>");
			}
			buff.append("</tr>");
			
			i++;
		}
		return buff.toString();
	}
	
	public static String convertSwissName(String input) {
		if (input == null)
			return "";
		
		return "Swiss Round " + input.charAt(9) + ": "
				+ input.substring(11);
	}
	
	public static String getPgnRepresentation(Game game) {
		final StringBuffer pgnBuffer = new StringBuffer();

		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_EVENT + " "
				+ PGN.TOK_QUOTE + game.getEvent() + PGN.TOK_QUOTE
				+ PGN.TOK_TAG_END + "\n");
		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_SITE + " " + PGN.TOK_QUOTE
				+ game.getSite() + PGN.TOK_QUOTE + PGN.TOK_TAG_END + "\n");
		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_DATE + " " + PGN.TOK_QUOTE
				+ game.getDate() + PGN.TOK_QUOTE + PGN.TOK_TAG_END + "\n");
		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_ROUND + " "
				+ PGN.TOK_QUOTE + game.getRound() + PGN.TOK_QUOTE
				+ PGN.TOK_TAG_END + "\n");
		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_WHITE + " "
				+ PGN.TOK_QUOTE + game.getWhite() + PGN.TOK_QUOTE
				+ PGN.TOK_TAG_END + "\n");
		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_BLACK + " "
				+ PGN.TOK_QUOTE + game.getBlack() + PGN.TOK_QUOTE
				+ PGN.TOK_TAG_END + "\n");
		pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_RESULT + " "
				+ PGN.TOK_QUOTE + game.getResultStr() + PGN.TOK_QUOTE
				+ PGN.TOK_TAG_END + "\n");

		if (game.getWhiteEloStr() != null)
			pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_WHITE_ELO + " "
					+ PGN.TOK_QUOTE + game.getWhiteElo() + PGN.TOK_QUOTE
					+ PGN.TOK_TAG_END + "\n");
		if (game.getBlackEloStr() != null)
			pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_BLACK_ELO + " "
					+ PGN.TOK_QUOTE + game.getBlackElo() + PGN.TOK_QUOTE
					+ PGN.TOK_TAG_END + "\n");
		if (game.getEventDate() != null)
			pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_EVENT_DATE + " "
					+ PGN.TOK_QUOTE + game.getEventDate() + PGN.TOK_QUOTE
					+ PGN.TOK_TAG_END + "\n");
		if (game.getECO() != null)
			pgnBuffer.append(PGN.TOK_TAG_BEGIN + PGN.TAG_ECO + " "
					+ PGN.TOK_QUOTE + game.getECO() + PGN.TOK_QUOTE
					+ PGN.TOK_TAG_END + "\n");
		game.gotoStart();
		// print leading comments before move 1
		String comment = game.getComment();
		if (comment != null) {
			pgnBuffer.append(PGN.TOK_COMMENT_BEGIN + comment
					+ PGN.TOK_COMMENT_END + " ");
		}

		game.traverse(new GameListener() {
			private boolean needsMoveNumber = true;

			public void notifyMove(Move move, short[] nags, String comment,
					int plyNumber, int level) {
				if (needsMoveNumber) {
					if (move.isWhiteMove()) {
						pgnBuffer.append(Chess.plyToMoveNumber(plyNumber) + "."
								+ " ");
					} else {
						pgnBuffer.append(Chess.plyToMoveNumber(plyNumber)
								+ "..." + " ");
					}
				}
				pgnBuffer.append(move.toString() + " ");

				if (nags != null) {
					for (int i = 0; i < nags.length; i++) {
						pgnBuffer.append(String.valueOf(PGN.TOK_NAG_BEGIN)
								+ String.valueOf(nags[i]) + " ");
					}
				}
				if (comment != null)
					pgnBuffer.append(PGN.TOK_COMMENT_BEGIN + comment
							+ PGN.TOK_COMMENT_END + " ");
				needsMoveNumber = !move.isWhiteMove() || (comment != null);
			}

			public void notifyLineStart(int level) {
				pgnBuffer.append(String.valueOf(PGN.TOK_LINE_BEGIN));
				needsMoveNumber = true;
			}

			public void notifyLineEnd(int level) {
				pgnBuffer.append(String.valueOf(PGN.TOK_LINE_END) + " ");
				needsMoveNumber = true;
			}
		}, true);

		pgnBuffer.append(game.getResultStr());

		return pgnBuffer.toString();
	}

	public static String getGuestsTableHtml(List<SwissGuest> swissGuests) {
		StringBuffer buff = new StringBuffer();
		//int maxRows = members.size()/4;
		buff.append("<table border=\"0\" align=\"center\">");
		int coloumn = 0;

		for (SwissGuest m : swissGuests) {
			if (coloumn == 0)
				buff.append("<tr>");
			else if (coloumn == 3) {
				buff.append("</tr>");
				coloumn = 0;
			}
			
			buff.append("<td width=\"25%\">");
			buff.append("<img src=\"http://simile.mit.edu/exhibit/examples/flags/images/" +
					""+m.getCountry().toLowerCase()+".png\" border=\"0\"/>");
			
			buff.append("<a href=\"/wiki/User:" + m.getUsername() + "\">"
						+ m.getUsername() + "</a>");
			
			buff.append("</td>");

			coloumn++;
		}
		buff.append("</table>");
		return buff.toString();
	}

}
