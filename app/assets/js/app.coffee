
window.ASSETS = $('body').data('assets-path')
window.USER = $('body').data('user')

angular.module('app', [
    'ui.bootstrap',
    'ngRoute'
    'app.filters'
    'app.services'
    'app.directives'
    'app.controllers'
]).config([ '$routeProvider', '$locationProvider',   
    ($routeProvider, $locationProvider) ->
        $routeProvider.when('/app', {templateUrl: "#{ASSETS}partials/start.html", controller: 'StartCtrl'});
        if (!USER)        
            $routeProvider.when('/app/signup', {templateUrl: "#{ASSETS}partials/signup.html", controller: 'SignupCtrl'})
            $routeProvider.when('/app/login', {templateUrl: "#{ASSETS}partials/login.html", controller: 'LoginCtrl'})
        $routeProvider.when('/app/about', {templateUrl: "#{ASSETS}partials/about.html"})
        
        $routeProvider.otherwise({redirectTo: '/'});

        $locationProvider.html5Mode(true);

])