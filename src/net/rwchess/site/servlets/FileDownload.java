package net.rwchess.site.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.datastore.Blob;

import net.rwchess.site.data.DAO;

public class FileDownload extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		String fileName = req.getRequestURI().substring(7);
		
		try {
			Blob file = DAO.getFile(fileName);
			if (file == null) {
				res.sendError(404);
				return;			
			}
			
			if (fileName.endsWith(".zip"))
				res.setContentType("application/zip");
            else if (fileName.endsWith(".pgn"))
				res.setContentType("application/x-chess-pgn");
            else if (fileName.endsWith(".ipg"))
				res.setContentType("image/jpeg");
			
			res.getOutputStream().write(file.getBytes());
			res.getOutputStream().flush();
		} 
		catch (IndexOutOfBoundsException e) {
			res.sendError(404);			
		}
	}
}
