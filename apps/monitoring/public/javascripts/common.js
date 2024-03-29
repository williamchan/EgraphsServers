var Egraphs = Egraphs || {}

Egraphs.displayPage = function (headerTitle, metricsUrl) {

  ajaxRequest(producePageStructure);
  ajaxRequest(scheduleTask);

  function producePageStructure (series) {
    pageStructureHelper(series, headerTitle);
  };

  function scheduleTask (series) {
    var metrics = series.metrics.length;
    for (var i=0; i<metrics; i++) {
      // all metrics update every minute
      setInterval(generateAjaxRequest, 60000);        
    };
  }; 

  function generateAjaxRequest () {
    ajaxRequest(updateAllGraphs);
  };

  function ajaxRequest (myFunction) {
    $.ajax({
		  url: metricsUrl,
		  method: 'GET',
		  dataType: 'json',
		  success: myFunction
	  });
  };
}

function pageStructureHelper (series, headerTitle) {
  var metrics = series.metrics.length;
  var temp;

  var table = document.createElement('table');
  document.getElementsByTagName('body')[0].appendChild(table);

  for (var i=0; i<metrics; i++) {
    if (i % 3 == 0) {
      var tr = document.createElement('tr');
      document.getElementsByTagName('table')[0].appendChild(tr);
    }
    var td = document.createElement('td');
    document.getElementsByTagName('tr')[Math.floor(i/3)].appendChild(td);  
    temp = document.createElement('h1');
    temp.className = 'metrics';
    temp.innerHTML = headerTitle + series.metrics[i].name + ":";
    document.getElementsByTagName('td')[i].appendChild(temp);
    
    var placeholder = document.createElement('div');
    placeholder.id = "placeholder-" + series.metrics[i].name; 
    placeholder.style.width = "90%";
    placeholder.style.height = "300px";
    document.getElementsByTagName('td')[i].appendChild(placeholder);
    
    // take care of initial graph drawing
    updateGraph(series.metrics[i]);
  };
};

function updateAllGraphs (series) {
  for (var k=0; k<series.metrics.length; k++) {
    updateGraph(series.metrics[k]);
  };
};

function updateGraph (metric) {
  // set up graph display options
  var options = {
		lines: { show: true },
		points: { show: true },
		xaxis: { tickDecimals: 0, tickSize: 5 },
		yaxis: { min: 0.0 }
	};

  // transform data into graphable points
	var newData = metric.values;
	var newDataTransformed = new Array(newData.length);
	for (var j=0; j<newData.length; j++) {
		newDataTransformed[j] = new Array(j+1, newData[j]);
	}

	var pageName = metric.name;
	var placeholder = $("#placeholder-" + pageName);

	$.plot(placeholder, [ newDataTransformed ], options);
};

function redirect(urlSuffix) {
  window.location = "/" + urlSuffix;
};
