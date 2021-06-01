(function() {

	var Billboard = 
		zul.billboard.Billboard = zk.$extends(zk.Widget,
		{

			_title : '',
			_type : 'line',
			
			_cursor : false,
			_highlighter : true,
			_stackSeries : false,
			_dataClickTS : 0,
			
			$define : {
				title: null,
				type: null,
				
				width: null,
				height: null,
				
				model : null,
				series : null,
				seriesData : null,
				seriesDefaults : null,
				seriesColors: null,
				axes : null,
				ticks : null,
				tickAxisLabel: null,
				valueAxisLabel: null,
				
				orient : null,
				rendererOptions: null,
				legend: null,
				thoudsandsSeparator: null,
				decimalMark: null,
				timeSeries: null,
				timeSeriesInterval: null,
				timeSeriesFormat: null,
				timeSeriesMin: null,
				xAxisAngle: null				
			},
			
			_dataPrepare : function() {
				var dataModel = this.getModel();
				var data = [];
				try {
					data = jq.evalJSON(dataModel);
				} catch (error) {
					console.log(error);
				}
				if (typeof data == "undefined") {
					data = [];
				}
				
				// In this phase, we need to decide following variables
				var seriesData = [];
				var ticks = [];
				
				// Start data prepare
				if( this.getType() == 'gauge') {
					seriesData.push("data");						
					for ( var i = 0, len = data.length; i < len; i++) {
						seriesData.push(data[i]['value']);
					}
				} else if( this.getType() == 'pie' || this.getType() == 'donut') {
					for ( var i = 0, len = data.length; i < len; i++) {
						seriesData.push(data[i]['category']);
						seriesData.push(data[i]['value']);
					}
					
					seriesData = [ seriesData ];
					
				} else {
					var seriesMap = new Array();
					for ( var i = 0, len = data.length; i < len; i++) {

						var current = data[i];
						var seriesIndex = -1;
						var seriesLabel = current['series'];
						var categoryLabel = current['category'];
						var categoryValue = current['value'];
						var seriesIndex = seriesMap.indexOf(seriesLabel);
						if (seriesIndex < 0) {
							seriesMap.push(seriesLabel);
							seriesIndex = seriesMap.length-1;
						}										
						// Initial Array
						if(!seriesData[seriesIndex]) {
							seriesData[seriesIndex] = new Array();
						}
						
						if (this.getType() == 'stacked_bar' || this.getType() == 'waterfall') {
							seriesData[seriesIndex].push(categoryValue);
							if (ticks.indexOf(categoryLabel) < 0) {
								ticks.push(categoryLabel);
							}
						} else {
							if (seriesData[seriesIndex].length == 0)
								seriesData[seriesIndex].push(categoryLabel);
							seriesData[seriesIndex].push(categoryValue);
						}
						
					}
					
					var seriesLabel = new Array();
					for (var i=0; i<seriesMap.length; i++) {
						var label = seriesMap[i];
						seriesLabel.push({label: label});
					}
					this.setSeries(seriesLabel);
				}
				// End data prepare
				
				this.setSeriesData(seriesData);
				this.setTicks(ticks);
				
			},
			
			_chartPrepare : function() {
				
				var wgt = this;
				// In this phase, we need to decide following variables
				var axes = {};
				var seriesDefaults = {};
				
				seriesDefaults.rendererOptions = {};
				// Start chart prepare
				if(this.isBarType()) {											
					// Stack
					if(this._type == 'stacked_bar') {
						this._stackSeries = true;
					}
					
					if(this._type == 'waterfall') {
						seriesDefaults.rendererOptions = {
							waterfall: true,									
							showDataLabels: true
						};							
						seriesDefaults.pointLabels = {
			                show: true,
			                hideZeros: true,
			                formatString: '%\'#.2f'
			            };
					}		
					seriesDefaults.breakOnNull = true;
				} else if(this.getType() == 'area') {
					this.stackSeries = true;
					seriesDefaults.fill = true;
					seriesDefaults.fillAndStroke=true;
					seriesDefaults.fillToZero=true;
				} else if(this.getType() == 'pie') {
					seriesDefaults.rendererOptions.showDataLabels = true;
				} else if(this.getType() == 'donut') {
					seriesDefaults.rendererOptions.showDataLabels = true;								
				}

				if (this.getRendererOptions()) {						
					var options = jq.evalJSON(this.getRendererOptions());
					if (seriesDefaults.rendererOptions)
						seriesDefaults.rendererOptions = jQuery.extend({}, seriesDefaults.rendererOptions, options);
					else
						seriesDefaults.rendererOptions = options;
				}
				
				// Horizontal or Vertical ?
				if(this.getType() != 'pie' && this.getType() != 'dial' && this.getType() != 'ring') {
					var axisRenderer = this.getTimeSeries() ? "timeseries" : "category";
					
					// Vertical
					axes.xaxis = {
						renderer : axisRenderer ,
						ticks: wgt.getTicks()
					};
					if (this.getTimeSeries()) {
						axes.xaxis.tickInterval = this.getTimeSeriesInterval();
						axes.xaxis.tickOptions = {formatString: this.getTimeSeriesFormat()};
						axes.xaxis.min = this.getTimeSeriesMin();
					} 
					if (this.getXAxisAngle() != 0) {
						if (axes.xaxis.tickOptions) {
							axes.xaxis.tickOptions.angle = this.getXAxisAngle();
						} else {
							axes.xaxis.tickOptions = {
								angle: this.getXAxisAngle()
							};
						}
					}
					axes.yaxis = { autoscale : true, tickOptions: {formatString: '%\'#.2f'}};
					if (this.getTickAxisLabel()) {
						axes.xaxis.label = this.getTickAxisLabel(); 
					}
					if (this.getValueAxisLabel()) {
						axes.yaxis.label = this.getValueAxisLabel();
					}
					
					if(this.getOrient() == 'horizontal') {
						axes.rotated = true;
					}
				}
				
				// End chart prepare
				
				this.setAxes(axes);
				this.setSeriesDefaults(seriesDefaults);
			},
			
			_chartPlot : function() {
				var wgt = this;
				var legend = this.getLegend() ? jq.evalJSON(this.getLegend()) : null;
				var seriesColors = this.getSeriesColors() ? jq.evalJSON(this.getSeriesColors()) : null;
				var nodata = false;
				if (typeof wgt.getSeriesData() == "undefined" || wgt.getSeriesData() == null) {
					nodata = true;
				} else if (wgt.getSeriesData().length == 0){						
					nodata = true;
				} else if (Object.prototype.toString.call(wgt.getSeriesData()[0]) === '[object Array]') {
					var count = 0;
					for(var i = 0; i < wgt.getSeriesData().length; i++) {
						count = count + wgt.getSeriesData()[i].length;
					}
					nodata = (count == 0);
				}
				if (!nodata) {
					var c = zk.$import('zul.billboard.Billboard');
					bb.generate(c._renderers[wgt._type].render(wgt));
				}
			},

			bind_ : function() {
				
				this.$supers(Billboard, 'bind_', arguments);
				
				// Step 1
				this._dataPrepare();	
				
				// Step 2
				this._chartPrepare();
				
				// Step 3
				this._chartPlot();					
			},
			
			unbind_ : function() {
				this.$supers(Billboard, 'unbind_', arguments);
			},
			
			doClick_ : function(event) {
				var ts = new Date().getTime();
				if ((ts - this._dataClickTS) > 500)
					this.$supers(Billboard, 'doClick_', arguments);
			},
			
			isBarType : function() {
				return this._type == 'bar' || this._type == 'stacked_bar' || this._type == 'waterfall';
			},
			
			getCursor : function() {
				if(this._cursor) {
					return { show: true, followMouse: true, 
						showTooltip: true, tooltipLocation:'sw', style: 'pointer'};						
				} else {
					return { show: false };
				}
			},
			
			setCursor : function(val) {
				this._cursor = val; 
			},
			
			getZclass : function() {
				return this._zclass != null ? this._zclass : "z-billboard";
			}

		}, {_renderers : {}});
})();