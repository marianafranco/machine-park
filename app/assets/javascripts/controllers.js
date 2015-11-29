'use strict';

/* Controllers */

app.controller('HeaderCtrl', ['$scope', '$location',
	function($scope, $location) {

		if ($location.path() === '/challenge2') {
			$scope.selectedChallenge1 = false;
		} else {
			$scope.selectedChallenge1 = true;
		}

		$scope.selected = function (page) {
			if (page === 'challenge2') {
				$scope.selectedChallenge1 = false;
			} else {
				$scope.selectedChallenge1 = true;
			}
		}
	}
]);

app.controller('HomeCtrl', ['$scope', '$http',
	function($scope, $http) {

	    var wsUrl = jsRoutes.controllers.MachineParkController.socket().webSocketURL();

		$scope.machines = [];
		$scope.types = ["mill", "3d-printer", "lathe", "saw", "water-cutter", "laser-cutter"];

		$scope.filterOnlyAlertMachines = function (machine) {
			if ($scope.onlyAlertMachines) {
				return machine.current > machine.current_alert;
			} else {
				return true;
			}
        };

	    function listener(data) {
//	        console.log("Received data from websocket: ", data);
	        var exists = false;
	        for (var i = 0; i < $scope.machines.length; i++) {
	        	if ($scope.machines[i].name === data.name) {
	        		exists = true;
	        		$scope.machines[i] = data;
	        	}
	        }
	        if (!exists) {
	        	$scope.machines.push(data);
	        }
	        $scope.$apply();
	    }

		var connect =  function () {
			var ws = new WebSocket(wsUrl);
			ws.onmessage = function(message) {
				listener(JSON.parse(message.data));
			};
			ws.onclose = function() {
				console.log('Socket connection closed... trying to reconnect.');
				setTimeout(function () {
					connect();
				}, 5000);
			}
		}
		connect();


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

app.controller('Challenge2Ctrl', ['$scope',
	function($scope) {

	}
]);