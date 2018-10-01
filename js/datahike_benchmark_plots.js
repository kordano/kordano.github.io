function createBarChart(canvasID, graphData, yAxisType, yAxisTitle, xAxisTitle, title) {

  var ctx = document.getElementById(canvasID).getContext('2d');

  return new Chart(ctx, {
    // The type of chart we want to create
    type: 'line',

    // The data for our dataset
    data: {
      labels: ["10K", "100K", "1M"],
      datasets: [{
        label: "datahike",
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        borderColor: 'rgba(75, 192, 192)',
        borderWidth: 1,
        data: graphData[0],
      },{
        label: "datomic",
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        borderColor: 'rgba(255, 99, 132)',
        borderWidth: 1,
        data: graphData[1],
      }, {
        label: "datascript",
        backgroundColor: 'rgba(153, 102, 255, 0.2)',
        borderColor: 'rgba(153, 102, 255)',
        borderWidth: 1,
        data: graphData[2],
      }]
    },

    // Configuration options go here
    options: {
      scales: {
        yAxes: [{
          type: yAxisType,
          ticks: {
            autoSkip: true,
          },
          scaleLabel: {
            labelString: yAxisTitle,
            display: true
          }
        }],
        xAxes: [{
          scaleLabel: {
            labelString: xAxisTitle,
            type: "linear",
            display: true
          }
        }],
      },
      legend: {
        display: true
      },
      title: {
        display: true,
        text: title
      }
    }
  });
}

var insertionData = [[13715.26422, 133053.05744, 1351278.91231], [503.276519, 2882.789454, 7001.234535], [851.469728, 2299.92175, 5901.234535]]
var insertionChart = createBarChart("insertionChart", insertionData, "linear", "Execution time in milliseconds", "Sample size", "Data Insertion Performance")

var queryData1 = [[62.566438, 344.492521, 490.123], [104.443381, 95.701976, 102.234461], [22.049357, 22.313238, 23.112649]]
var queryChart1 = createBarChart("queryChart1", queryData1, "linear", "Execution time in microseconds", "Sample size", "Basic indexed query")

