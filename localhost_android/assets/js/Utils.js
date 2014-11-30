
function Utils() {

};

/*
 * ----->
 */

Utils.PASSWORD_KEY = "password_key";

Utils.savePassword = function(cpass) {
$.cookie(Utils.PASSWORD_KEY,cpass);
};

Utils.getPassword = function() {
return $.cookie(Utils.PASSWORD_KEY);
};

Utils.removePassword = function()
{
$.removeCookie(Utils.PASSWORD_KEY );
};

/*
 * ----->
 */

Utils.include = function(filename)
{
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.src = filename;
    script.type = 'text/javascript';
    head.appendChild(script)
}


