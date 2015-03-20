/* toto-new-user.js */


function checkPasswords()
{
    var pwd1 = $( "#password1" ).val().trim();
    var pwd2 = $( "#password2" ).val().trim();


    var errDiv = $("#error");
    
    if ((pwd1.length > 0) && (pwd2.length > 0) && (pwd1 != pwd2))
        errDiv.text("Passwords do not match");
    else
        errDiv.empty();

        
}

$(document).ready(function () {
    $( "#password1" ).keyup(checkPasswords);
    $( "#password1" ).change(checkPasswords);
    
    $( "#password2" ).keyup(checkPasswords);
    $( "#password2" ).change(checkPasswords);
});
