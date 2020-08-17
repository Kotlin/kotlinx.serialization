document.getElementById("navigationFilter").oninput = function (e) {
	var input = e.target.value;
	var menuParts = document.getElementsByClassName("sideMenuPart")
	for (let part of menuParts) {
    	if(part.querySelector("a").textContent.startsWith(input)) {
    		part.classList.remove("filtered");
    	} else {
    		part.classList.add("filtered");
    	}
	}
}