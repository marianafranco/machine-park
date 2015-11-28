'use strict';

/* Controllers */

app.controller('HomeCtrl', ['$scope', '$http',
	function($scope, $http) {

	    var getMachines = function () {
            $http.get("/machines")
            .success(function (data, status, headers) {
                console.log("Successfully list all machines", data);
                $scope.machines = data;
            }).error(function (data, status, headers) {
                console.error("Failed to list machines. Status:" + status);
            });
	    };

	    getMachines();
	}
]);