angular.module('app.controllers', ['ngRoute'])
    .controller('MainCtrl', ['$scope', '$route', '$routeParams', '$location',
        class MainCtrl
            constructor: ($scope, $route, $routeParams, $location) ->
                $scope.$route = $route
                $scope.$location = $location
                $scope.$routeParams = $routeParams
                $scope.user = $('body').data('user')
                if ($scope.user) 
                    $scope.loggedIn = true
                else 
                    $scope.loggedIn = false
    ])
    .controller('StartCtrl', ['$scope',
        class StartCtrl
            constructor: ($scope) ->
                $scope.ctrl = 'StartCtrl'
    ])
    .controller('SignupCtrl', ['$scope', '$log',
        class SignupCtrl
            constructor: (scope, log) ->
                scope.ctrl = 'SignupCtrl'
                scope.username = ""
                scope.password = ""
                scope.confirm = ""
                scope.submitForm = (isValid) ->
                    if (isValid)
                        log.debug("SIGNUP: #{scope.username}, #{scope.password}")


    ])
    .controller('LoginCtrl', ['$scope', '$log', '$http', '$window',
        class LoginCtrl
            constructor: (scope, log, http, $window) ->
                scope.ctrl = 'LoginCtrl'
                scope.username = ""
                scope.password = ""
                scope.submitForm = () ->
                    log.debug("LOGIN: #{scope.username}, #{scope.password}")
                    http({
                        method: 'POST',
                        url: '/api/login'
                        data: {
                            username: scope.username
                            password: scope.password
                        }
                    }).success((data, status, headers) ->
                        log.debug("Logged in")
                        window.location.href = "/app"
                    ).error((data, status, headers) ->
                        log.debug("Failed to log in")
                        $window.alert("Failed to login")
                    )
    ])
    .controller('GameListCtrl', ['$scope', '$log', '$http',
            class GameListCtrl
                constructor: (scope, log, http) ->
                    scope.ctrl = 'GameListCtrl'
                    games = []
                    for i in [1..100] 
                        games.push {
                            name: i
                        }
                    scope.shownGames = []
                    scope.totalGames = games.length
                    scope.gamesPerPage = 2
                    scope.currentPage = 1
                    scope.limitPages = 10
                    resliceGames = ->
                        init = (scope.currentPage-1)*scope.gamesPerPage
                        limit = init+scope.gamesPerPage
                        log.debug("init: #{init} limit: #{limit}")
                        scope.shownGames = games.slice(init, limit)
                    scope.$watch 'currentPage', (newValue, oldValue) ->
                        log.debug("Current page: #{newValue} .. old: #{oldValue}")
                        resliceGames()
        ])