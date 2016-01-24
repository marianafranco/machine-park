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

app.controller('HomeCtrl', ['$scope', '$uibModal',
	function($scope, $uibModal) {

	    var wsUrl = jsRoutes.controllers.MachineParkController.socket().webSocketURL();
	    var alertsUrl = jsRoutes.controllers.MachineParkController.alertSocket().webSocketURL();

		$scope.machines = [];
		$scope.types = ["mill", "3d-printer", "lathe", "saw", "water-cutter", "laser-cutter"];

		$scope.alerts = [];
		$scope.newAlertsCount = 0;

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
	        		$scope.machines[i].current = data.current;
	        		$scope.machines[i].current_alert = data.current_alert;
	        		$scope.machines[i].state = data.state;
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

		function listenerAlerts(data) {
			$scope.alerts.push(data);
			$scope.newAlertsCount++;
		}

		var connectAlertSocket =  function () {
			var ws = new WebSocket(alertsUrl);
			ws.onmessage = function(message) {
				listenerAlerts(JSON.parse(message.data));
			};
			ws.onclose = function() {
				console.log('Socket connection closed... trying to reconnect.');
				setTimeout(function () {
					connect();
				}, 5000);
			}
		}
		connectAlertSocket();


		$scope.timestampToDate = function (timestamp) {
			var dt = new Date(timestamp);

			var hour = dt.getHours() < 10 ? '0' + dt.getHours() : dt.getHours();
			var min = dt.getMinutes() < 10 ? '0' + dt.getMinutes() : dt.getMinutes();
			var sec = dt.getSeconds() < 10 ? '0' + dt.getSeconds() : dt.getSeconds();

			var day =  dt.getDate() < 10 ? '0' + dt.getDate() : dt.getDate();
			var month = dt.getMonth() + 1;
			month = month < 10 ? '0' + month : month;

			return day + '/' + month + '/' + dt.getFullYear() + ' ' + hour + ':' + min + ':' + sec;
		};

		$scope.cleanNewAlertsCount = function() {
			$scope.newAlertsCount = 0;
		};

		$scope.openModal = function(machineName) {
            var modalInstance = $uibModal.open({
              animation: false,
              templateUrl: '/assets/partials/modal.html',
              controller: 'ModalInstanceCtrl',
              size: 'sm',
              resolve: {
                machineName: function () {
                  return machineName;
                }
              }
            });
		};
	}
]);

app.controller('ModalInstanceCtrl', ['$scope', '$http', '$uibModalInstance', 'machineName',
	function ($scope, $http, $uibModalInstance, machineName) {

		$scope.machineName = machineName;

		$scope.correlation = {
			temperature : 1,
			pressure: 0,
			humidity: -1
		}

		$scope.ok = function () {
			$uibModalInstance.close();
		};

		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
	}
]);

app.controller('Challenge2Ctrl', ['$scope',
	function($scope) {

	}
]);