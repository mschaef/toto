<html>
<head>
<link rel="stylesheet" type="text/css" href="resources/todo.css" />

<script type="text/javascript" src="resources/jquery-1.7.1.js"></script>

<script type="text/javascript">
</script>

<title>Login to Toto</title>

<head>

<body>

<h3>Login</h3>

<form id="login" action="/login" method="post">
  User Name: <input type="text" name="username" id="username">
<br>
  Password: <input type="text" name="password" id="password">
<br>
          <input type="submit" value="Submit" />
</form>

<#if failed??>
   <font color="red">Login Failed</a>
</#if>

<a href="/add-user">Add a new user account</a>

</body>
</html>
