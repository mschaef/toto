<html>
<head>
<link rel="stylesheet" type="text/css" href="resources/todo.css" />

<script type="text/javascript" src="resources/jquery-1.7.1.js"></script>

<script type="text/javascript">
</script>

<title>Add a User</title>

<head>

<body>

<h3>Add a User</h3>

<form id="add-user" action="/add-user" method="post">
  User Name: <input type="text" name="username" id="username">
<br>
  Password: <input type="text" name="password1" id="password1">
<br>
  Repeat Password: <input type="text" name="password2" id="password2">
<br>
  e-mail: <input type="text" name="email" id="email">
<br>
    <input type="submit" value="Submit" />
</form>

<#if failed??>
   <font color="red">${failed}</a>
</#if>

</body>
</html>
