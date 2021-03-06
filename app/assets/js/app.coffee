
window.ASSETS = $('body').data('assets-path')
window.USER = $('body').data('user')

angular.module('app', [
    'ui.bootstrap',
    'ngRoute'
    'app.filters'
    'app.services'
    'app.directives'
    'app.controllers'
    'app.stratego'
]).config([ '$routeProvider', '$locationProvider', '$httpProvider',  
    ($routeProvider, $locationProvider, $httpProvider) ->
        $routeProvider.when('/app', {templateUrl: "#{ASSETS}partials/play.html", controller: 'PlayCtrl'});
        if (!USER)    
            $routeProvider.when('/app/signup', {templateUrl: "#{ASSETS}partials/signup.html", controller: 'SignupCtrl'})
            $routeProvider.when('/app/login', {templateUrl: "#{ASSETS}partials/login.html", controller: 'LoginCtrl'})
        $routeProvider.when('/app/about', {templateUrl: "#{ASSETS}partials/about.html"})
        if (USER) 
            $routeProvider.when('/app/leaderboard', {templateUrl: "#{ASSETS}partials/leaderboard.html", controller: 'LeaderboardCtrl'})
            $routeProvider.when('/app/replays', {templateUrl: "#{ASSETS}partials/replays.html", controller: 'ReplaysCtrl'})
        if (USER and USER.admin)
            $routeProvider.when('/app/admin', {templateUrl: "#{ASSETS}partials/admin.html", controller: 'AdminCtrl'})

        $routeProvider.otherwise({redirectTo: '/app'});

        $locationProvider.html5Mode(true);

        $httpProvider.defaults.headers.post["Csrf-Token"] = "nocheck"

])