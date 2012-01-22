<html>
<head>
<script type="text/javascript" src="resources/jquery-1.7.1.js"></script>

<script type="text/javascript">
function completeItem(id)
{
$.post("/complete", { itemId:id });
}

function removeItem(id)
{
$.post("/remove", { itemId:id} );
}
</script>

<head>

<body>

<h3>Todo Items</h3>

<table border="0">
  <#list todoItems as item>
    <tr>
      <td>${item.id}</td>
      <td>${item.description}</td>
      <td><div onclick="completeItem(${item.id})" style="cursor:pointer">Complete</div></td>
      <td><div onclick="removeItem(${item.id})" style="cursor:pointer">Remove</div></td>
    </tr>
  </#list>
</table>

<form id="todo" action="/todo" method="post">
   <input type="text" name="desc" id="desc">
   <input type="submit"/>
</form>

</body>
</html>
