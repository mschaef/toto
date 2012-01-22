<html>
<body>
<h3>Todo Items</h3>

<table border="0">
  <#list todoItems as item>
    <tr>
      <td>${item.description}</td>

    </tr>
  </#list>
</table>

<form id="todo" action="/todo" method="post">
   <input type="text" name="desc" id="desc">
   <input type="submit"/>
</form>

</body>
</html>
