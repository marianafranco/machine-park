'use strict';

/* Controllers */

app.controller('HomeCtrl', ['$scope', '$http',
	function($scope, $http) {

	    var wsUrl = jsRoutes.controllers.MachineParkController.socket().webSocketURL();

		$scope.machines = {};
		$scope.numOfMachines = 0;

	    function listener(data) {
	        console.log("Received data from websocket: ", data);
	        $scope.machines[data.name] = data;
	    }

	    var ws = new WebSocket(wsUrl);
		ws.onmessage = function(message) {
			listener(JSON.parse(message.data));
		};

//	    var getMachines = function () {
//            $http.get("/machines")
//            .success(function (data, status, headers) {
//                console.log("Successfully list all machines", data);
//                for (var i = 0; i < data.length; i++) {
//                	$scope.machines[data.name] = data;
//                }
//
//                var ws = new WebSocket(wsUrl);
//                ws.onmessage = function(message) {
//					listener(JSON.parse(message.data));
//				};
//            }).error(function (data, status, headers) {
//                console.error("Failed to list machines. Status:" + status);
//            });
//	    };
//
//	    getMachines();
	}
]);