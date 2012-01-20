<html>
<body>
<h3>Todo Items</h3>

<list>
  <#list todoItems as item>
    <li>${item.description}
  </#list>
</list>

<form id="todo" action="/todo" method="post">
   <input type="text" name="desc" id="desc">
   <input type="submit"/>
</form>

</body>
</html>
