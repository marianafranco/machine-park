'use strict';

/* App Module */

var app = angular.module('machineParkApp', ['ngRoute', 'ui.bootstrap']);

app.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/', {
        templateUrl: '/assets/partials/challenge1.html',
        controller: 'HomeCtrl'
      }).
      when('/challenge2', {
        templateUrl: '/assets/partials/challenge2.html',
        controller: 'Challenge2Ctrl'
      }).
      otherwise({
        redirectTo: '/'
      });
  }
]);