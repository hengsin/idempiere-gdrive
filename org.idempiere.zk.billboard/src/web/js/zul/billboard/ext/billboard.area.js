var billboard = billboard || {};

billboard.AreaRenderer = function() {};

billboard.AreaRenderer.prototype.render = function(wgt) {
	var columns = [["data"]];
	var categories = new Array();
	wgt.getSeriesData().forEach((x, i) => { 
		x.forEach((y, j) => {
			if (y.category) {
				columns[0].push(y.value);
				if (!categories.indexOf[y.category])
					categories.push(y.category);
			}
		});
	});
	var color = {};
	var area = {};
	var background = {};
	var rendererOptions = wgt._rendererOptions ? jq.evalJSON(wgt._rendererOptions) : null;
	if (rendererOptions) {
		if (rendererOptions["intervalColors"]) {
			color["pattern"] = new Array();
			rendererOptions["intervalColors"].forEach((x, i) => color["pattern"].push(x));
		}
		if (rendererOptions["intervals"]) {
			color["threshold"] = {values: []};
			rendererOptions["intervals"].forEach((x, i) => color["threshold"]["values"].push(x));
		}

		if (rendererOptions["background"]) {
			background["color"] = rendererOptions["background"];
		}
	}
	var x = {tick: {}};
	var axes = wgt.getAxes();
	if (axes.xaxis.renderer == "timeseries") {
		x["type"] = "timeseries";
		if (axes.xaxis.tickOptions) {
			x["tick"]["format"] = axes.xaxis.tickOptions.formatString;
		}		
	} else {
		x["type"] = "category";
	}
	if (x["type"] == "category") {
		if (categories.length > 0) {
			x["categories"] = [];
			categories.forEach((v, i) => x["categories"].push(v));
		}
	} else if (x["type"] == "timeseries") {
		x["tick"]["values"] = [];
		wgt.getSeries().forEach((s, i) => x["tick"]["values"].push(s.label));
	}
	
	var model = { 
		bindto: "#"+wgt.$n().id, 
		data: { 
			columns: columns, 
			type: wgt._type,
			onclick: function(d, e) {
				wgt._dataClickTS = new Date().getTime();
				wgt.fire("onDataClick", {
					seriesIndex : d.x,
					pointIndex : d.index,
					data : d.value,
					ticks : wgt.getTicks()
				});
			}
		},
		color: color,
		area: area,
		tooltip: {
			show: true,
		    doNotHide: false,
		    grouped: false,
		    contents: function(d, defaultTitleFormat, defaultValueFormat, color) {
				var c = d[0];
				var h = '<table class="bb-tooltip"><tbody><tr><th>';
				h = h + categories[c.x];
				h = h + '</th></tr><tr class="bb-tooltip-name-data"><td class="value">';
				h = h + c.value + '</td></tr></tbody></table>';
		        return h;
		    }
		},
		legend: {show: false},
		axis: {
			x: x
		},
		grid: {
		  x: {
		    show: true,
		  },
		  y: {
		    show: true,
		  },
		  front: false,
		  focus: {
		     show: false,
		
		     // Below options are available when 'tooltip.grouped=false' option is set
		     edge: true,
		     y: true
		  },
		  lines: {
		     front: false
		  }
		}
	};
	if (wgt.getTitle())
		model["title"] = {text: wgt.getTitle()};
	if (wgt.getTickAxisLabel())
		model["axis"]["x"]["label"] = {text: wgt.getTickAxisLabel(), position: "outer-right"}; 
	if (wgt.getValueAxisLabel())
		model["axis"]["y"] = { label: {text: wgt.getValueAxisLabel(), position: "outer-top"} };
	return model;
};

zul.billboard.Billboard._renderers["area"] = new billboard.AreaRenderer();
		