angular.module('app.controllers', ['ngRoute', 'app.stratego'])
    .controller('MainCtrl', ['$scope', '$route', '$routeParams', '$location', '$http', '$log',
        class MainCtrl
            constructor: (scope, $route, $routeParams, $location, http, log) ->
                scope.$route = $route
                scope.$location = $location
                scope.$routeParams = $routeParams
                scope.user = $('body').data('user')
                scope.templateUrls = {
                    currentGame: "#{ASSETS}partials/play/_current_game.html"
                    gamesList:"#{ASSETS}partials/play/_game_list.html"
                    navbar: "#{ASSETS}partials/_navbar.html"
                }
                if (scope.user) 
                    scope.loggedIn = true
                else 
                    scope.loggedIn = false
                scope.logout = () ->
                    log.debug('logout')
                    if(scope.loggedIn) 
                        http({
                            method: 'POST',
                            url: '/api/logout'  
                        }).success(->
                            window.location.href = "/app"
                            window.reload(true)
                        ).error(->
                            log.error("Can't log out")
                        )
                    else
                        log.debug("Already logged out")
                scope.isActive = (url) ->
                    url == $location.path()

    ])
    .controller('PlayCtrl', ['$scope',
        class PlayCtrl
            constructor: (scope) ->
                scope.ctrl = 'PlayCtrl'
                scope.currentGame=true
    ])
    .controller('SignupCtrl', ['$scope', '$log', '$http'
        class SignupCtrl
            constructor: (scope, log, http) ->
                scope.ctrl = 'SignupCtrl'
                scope.username = ""
                scope.password = ""
                scope.confirm = ""
                scope.submitForm = (isValid) ->
                    if (isValid)
                        log.debug("SIGNUP: #{scope.username}, #{scope.password}")
                        http({
                            method: 'POST',
                            url: '/api/signup',
                            data: {
                                username: scope.username,
                                password: scope.password    
                            }    
                        }).success(->
                            log.debug("Signed up")
                            window.location.href = "/app"
                            window.reload(true)
                        ).error(->
                            log.error("Failed to log in")
                            window.alert('Failed to log in')
                        )

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
                interval = window.setInterval((() ->
                    log.debug('List poll')
                ), 5000)
                for i in [1..3] 
                    games.push {
                        id: "12345"
                        creator: {
                            id: "54321"
                            username: 'cbrahms2'
                        }
                        name: i
                        state: "PENDING"
                    }
                scope.shownGames = []
                scope.totalGames = games.length
                scope.gamesPerPage = 2
                scope.currentPage = 1
                scope.limitPages = 10
                scope.createGame = -> log.info("GameListCtrl - createGame")
                
                resliceGames = ->
                    init = (scope.currentPage-1)*scope.gamesPerPage
                    limit = init+scope.gamesPerPage
                    log.debug("init: #{init} limit: #{limit}")
                    scope.shownGames = games.slice(init, limit)
                
                scope.$watch 'currentPage', (newValue, oldValue) ->
                    log.debug("Current page: #{newValue} .. old: #{oldValue}")
                    resliceGames()
                scope.$on('$destroy', (->
                    window.clearInterval(interval)
                ))

    ])
    .controller('CurrentGameCtrl', ['$scope', '$log', '$http', 'StrategoFactory',
        class CurrentGameCtrl
            constructor: (scope, log, http, StrategoFactory) ->
                scope.ctrl = "CurrentGameCtrl"
                scope.liveGame = false
                # angular.element('canvas').ready () =>
                #     # @controller = new StrategoFactory.StrategoController('534f32eeb9683ad8632221a9')
                #     @board = new StrategoFactory.StrategoBoard(canvas: 'canvas', isRed: true)
                #     @board.init().finally () =>
                #         @board.draw()
                #     @ctrl = new StrategoFactory.StrategoController(board: @board, gameId: "1234")
                #     @ctrl.start()
                #         .then ->
                #             log.debug("CurrentGameCtrl started game")
                
                scope.createGame = ->
                    log.debug("Creating game")
                    # http({
                    #     method: 'POST',
                    #     url: '/api/games/create',
                    #     data: {
                    #         type: 'stratego'
                    #     }
                    # }).success( (data) ->
                    #     scope.liveGame = true
                    #     scope.gameId = data.gameId
                    #     log.debug("Created game with id: " + data.gameId)
                        

                    # ).error(->
                    #     log.error("Unable to create game")
                    # )

    ])
    .controller('HistoryCtrl', ['$scope', '$log', 
        class HistoryCtrl
            constructor: (scope, log) ->
                scope.ctrl = 'HistoryCtrl'
    ])
    .controller('GameHistoryCtrl', ['$scope', '$log', '$http'
        class GameHistoryCtrl
            constructor: (scope, log, http) ->
                scope.ctrl = 'GameHistoryCtrl'

    ])
    .controller('PlayerStatisticsCtrl', ['$scope', '$log', '$http', 
        class PlayerStatisticsCtrl
            constructor: (scope, log, http) ->
                scope.ctrl = 'PlayerStatisticsCtrl'
    ])
    .controller('AdminCtrl', ['$scope', '$log', '$http',
        class AdminCtrl
            constructor: (scope, log, http) ->
                scope.ctrl = 'AdminCtrl'
    ])