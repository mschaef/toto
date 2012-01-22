<html>
<head>
<script type="text/javascript" src="resources/jquery-1.7.1.js"></script>

<script type="text/javascript">
 $(document).ready(function() {
   $("a").click(function() {
     alert("Hello world!");
   });
 });
</script>

<head>

<body>

<h3>Todo Items</h3>

<table border="0">
  <#list todoItems as item>
    <tr>
      <td>${item.id}</td>
      <td>${item.description}</td>
      <td><a href="">Complete</a></td>
      <td><a href="">Remove</a></td>
    </tr>
  </#list>
</table>

<form id="todo" action="/todo" method="post">
   <input type="text" name="desc" id="desc">
   <input type="submit"/>
</form>

</body>
</html>
