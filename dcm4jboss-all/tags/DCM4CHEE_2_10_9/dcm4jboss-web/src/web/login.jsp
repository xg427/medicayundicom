<%--
	$Id: login.jsp 3018 2006-12-19 11:09:55Z javawilli $
	$Source$
	
	Autologin for WebViewer:
	To allow external Webviewer access without login you can use request parameter to achieve autologin.
	1) accNr (Accession number) not null! 
	2) loginuser (user name)
	3) passwd4user (password for username in Base64)
	Be aware that this critical infos are visible in browser window!
	Therefore this user should only have WebUser role.
--%>
<%@ page import='org.dcm4che.util.Base64' %>
<html>
<head>
  <title>Login</title>
  <link href="style.css" rel="stylesheet" type="text/css">
</head>

<%
	String user = null;
	String passwd = null;
	if ( request.getParameter("accNr") != null || request.getParameter("studyUID") != null) {
		user = request.getParameter("loginuser");
		passwd = request.getParameter("passwd4user");
		if ( passwd != null && (passwd.length() % 4) == 0) {
			byte[] ba = Base64.base64ToByteArray(passwd);
			passwd = new String(ba);
		}
	}
	if ( user != null) {
%>
<body onload="document.login.submit()">
<% } else { 
	user = "";
	passwd="";
%>
<body onload="self.focus();document.login.j_username.focus()">
<% } %>
<table border="0" cellspacing="0" cellpadding="0" width="100%">
 <tr>
  <td><img src="white48.jpg" width="100%" height="5px"></td>
 </tr>
 <tr>
  <td background="white48.jpg">
    <img src="white48.jpg" width="10px" height="24px"><img src="logo.gif" alt="DCM4CHEE">
  </td>
 </tr>
 <tr>
  <td><img src="line.jpg" width="100%" height="20px" alt="line"></td>
 </tr>
</table>
<center>
<h1>User Login</h1>
<br>

<form name="login" method="POST" action="j_security_check">
<table>
	<tr valign="middle">
	  <td><div class="text">Name:</div></td>
	  <td><input class="textfield" type="text" name="j_username" value="<%=user %>"/></td>
	</tr>
	<tr valign="middle">
	  <td><div class="text">Password:</div></td>
	  <td><input class="textfield" type="password" name="j_password" value="<%=passwd %>"/></td>
	</tr>
	<tr><td>&nbsp;</td></tr>
	<tr valign="middle">
	  <td>&nbsp;</td>
	  <td align="center"><input class="button" type="submit" value="Log in"></td>
	</tr>
</table>
</center>
</form>
</body>
</html>
