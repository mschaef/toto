<html>
<head>
<link rel="stylesheet" type="text/css" href="resources/todo.css" />

<script type="text/javascript" src="resources/jquery-1.7.1.js"></script>

<script type="text/javascript">
  function reloadPage()
  {
     window.location.reload();
  }

  function completeItem(id)
  {
    $.post("/complete", { itemId:id }, reloadPage);
  }

  function removeItem(id)
  {
    $.post("/remove", { itemId:id }, reloadPage);
  }
</script>

<head>

<body>

<h3>Todo Items</h3>

<#function zebra index>
  <#if (index % 2) == 0>
    <#return "bgcolor=\"#e0e0e0\"" />
  <#else>
    <#return "" />
  </#if>
</#function>

<table border="0" width="100%" cellspacing="0" cellpadding="4">
  <#list todoItems as item>
    <tr ${zebra(item_index)}>
      <td width="100%">${item.description}</td>
      <td>
        <div onclick="completeItem(${item.id})" class="button">
          Complete
        </div>
      </td>
      <td>
        <div onclick="removeItem(${item.id})" class="button">
          Remove
        </div>
      </td>
    </tr>
  </#list>
    <tr>
      <td colspan="3">
        <form id="todo" action="/todo" method="post">
          <input type="text" name="desc" id="desc" style="width:100%">
        </form>
      </td>
    </tr>
</table>

<a href="/logout">Log out</a>

</body>
</html>
