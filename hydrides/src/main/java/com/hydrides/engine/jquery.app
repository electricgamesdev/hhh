get : function(name) {
	var  url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
},
path : function () {
   return window.location.pathname;//.substring(0, window.location.pathname.indexOf("/",2));
},
_destroy:function() {

},
_error:function(obj){
 					if($('#error')){
						$('#error').html(e.response);
	    				 }else{
            					alert('Error @ $intf.getOn()_$macro.getOn() : '+e.message);
       	    				 }
}