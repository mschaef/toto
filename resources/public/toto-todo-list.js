/* toto-todo-list.js */

$(document).ready(function () {
  $("#item-description").focus();

  $( ".item-list .item-row" ).draggable({
    appendTo: "body",
    helper: "clone"
  });

  $( ".item-list .item-row" ).dblclick(function (obj){
      beginItemEdit($(obj.delegateTarget).attr("itemid"));
  });

  $( ".list-list li" ).droppable({
    hoverClass: "drop-hover",
    accept: ":not(.ui-sortable-helper)",
    drop: function( event, ui ) {
      var itemId = $(ui.draggable[0]).attr("itemid");
      var newListId = $(this).attr("listid");

      $("#item_set_list_form #target-item")[0].value = itemId;
      $("#item_set_list_form #target-list")[0].value = newListId;
      $("#item_set_list_form")[0].submit();
      }
  });
});
