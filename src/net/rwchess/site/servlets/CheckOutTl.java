package net.rwchess.site.servlets;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Text;

import net.rwchess.site.data.DAO;
import net.rwchess.site.data.RssItem;
import net.rwchess.site.data.TeamDuel;
import net.rwchess.site.data.WikiEditObject;
import net.rwchess.site.utils.Mailer;
import net.rwchess.site.utils.TlPairingsParser;

public class CheckOutTl extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		/**try {
			URL url = new URL("http://teamleague.org/pairings.php");
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream()));
			
			TlPairingsParser tparser = new TlPairingsParser();
			tparser.parseFromReader(reader);
			
			PersistenceManager pm = DAO.get().getPersistenceManager();
			try {
				//pm.makePersistentAll(tparser.getDuels());
				
				for (TeamDuel d : tparser.getDuels()) {
					try {
						TeamDuel stored = (TeamDuel) pm.getObjectById(
								TeamDuel.class, d.getKey());
						stored.setResults(d.getResults());						
						stored.setFixated(d.getFixated());
					} 
					catch (JDOObjectNotFoundException e) {
						DAO.get().getPersistenceManager().makePersistent(d);
					}						
				}
			} 
			finally {
				pm.close();
				reader.close();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		} */
		
		List<WikiEditObject> objects = DAO.getAllWikiEditObjects();
		if (objects != null) {
			for (WikiEditObject wo : objects) {
				String msgBody = "The wiki page \""
						+ wo.getPageName()
						+ "\" has been edited on http://rwchess.appspot.com/wiki/"+wo.getPageName();
				RssItem rss = new RssItem();
				rss.setTitle("Wiki page edited: "+wo.getPageName());
				rss.setContent(new Text(msgBody));
				rss.setDate(new Date());
				DAO.deleteObj("RssFead");
				rss.setType("general");
				DAO.get().getPersistenceManager().makePersistent(rss);
				Mailer.editedWiki(msgBody, wo.getOriginalVersion(), wo
						.getChangedVersion(), wo.getUname());
				DAO.deleteWikiObj(wo.getPageName());
			}
		}
	}
}
