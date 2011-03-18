<%@ page import="net.rwchess.site.data.DAO"%>
<%@ page import="net.rwchess.site.utils.UsefulMethods"%>
<%@ page import="net.rwchess.site.data.TeamDuel"%>
<%@ page import="net.rwchess.site.data.T44Player"%>
<%@ page import="java.util.List"%>

<jsp:include page="/blocks/top.jsp"></jsp:include>
<jsp:include page="/blocks/currevents.jsp"></jsp:include>

<small>More information on <b></b><a href="/wiki/T42">wiki</a></b></small><br/><br/>

    
    <br/> 
	
   <% 
    String curr = "";   
    for (TeamDuel duel: DAO.getCurrRoundTeamDuels()) {
	   if (!duel.getSection().equals(curr)) {
	%>
		<table width="80%" border="1" cellpadding="0" cellspacing="2"><tr><td><div align="center"><%=duel.getSection() %></div></td></tr></table>
			
	<%	}
	   List<String> rmembers = duel.getRwPlayersList();
	   List<String> ropponents = duel.getOpponentPlayersList();
	   List<String> results = duel.getResults();
	   String first = duel.isWhiteFirst() ? "White" : "Black";
	   String second = duel.isWhiteFirst() ? "Black" : "White";
	   %>
	
	   <table width="80%"  border="1" cellpadding="0" cellspacing="2">
	    <tr bgcolor="#5e410f">
	       <td width="35%" align="right" colspan="2">&nbsp;<%=duel.getRwTeamname() %></td>
	       <td align="center">vs</td>
	       <td width="35%" colspan="2">&nbsp;<%=duel.getOpponentTeamname() %></td>
	    </tr> 
	    <tr>
	       <td width="14%">&nbsp;<%=first %></td>
	       <td width="20%">&nbsp;<%=rmembers.get(0) %></td>
	       <td width="13%" align="center"><%=results.get(0) %></td>
	       <td width="20%">&nbsp;<%=ropponents.get(0) %></td>
	       <td width="13%">&nbsp;<%=second %></td>
	    </tr>
	    <tr>
	       <td width="14%">&nbsp;<%=second %></td>
	       <td width="20%">&nbsp;<%=rmembers.get(1) %></td>
	       <td width="13%" align="center"><%=results.get(1) %></td>
	       <td width="20%">&nbsp;<%=ropponents.get(1) %></td>
	       <td width="13%">&nbsp;<%=first %></td>
	    </tr>
	    <tr>
	       <td width="14%">&nbsp;<%=first %></td>
	       <td width="20%">&nbsp;<%=rmembers.get(2) %></td>
	       <td width="13%" align="center"><%=results.get(2) %></td>
	       <td width="20%">&nbsp;<%=ropponents.get(2) %></td>
	       <td width="13%">&nbsp;<%=second %></td>
	    </tr>
	     <tr>
	       <td width="14%">&nbsp;<%=second %></td>
	       <td width="20%">&nbsp;<%=rmembers.get(3) %></td>
	       <td width="13%" align="center"><%=results.get(3) %></td>
	       <td width="20%">&nbsp;<%=ropponents.get(3) %></td>
	       <td width="13%">&nbsp;<%=first %></td>
	    </tr>
	   </table>
	   <br/>	     
<%  } %>


<jsp:include page="/blocks/bottom.jsp"></jsp:include>
