angular.module('app.controllers', ['ngRoute', 'app.stratego', 'ui.bootstrap'])
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
    .controller('PlayCtrl', ['$timeout', '$scope', '$log', '$http', '$modal',
        class PlayCtrl
            constructor: (timeout, scope, log, http, modal) ->
                scope.ctrl = 'PlayCtrl'
                scope.currentGameId=USER.currentGameId or null
                log.info("current user game _id: " + scope.currentGameId)
                scope.createGame = -> 
                    http({
                        method: 'POST'
                        url: "/api/games/create"
                        data: {
                            type: 'stratego'
                        }
                    }).success( (data) ->
                        USER.currentGameId = scope.currentGameId = data._id
                        modal.open {
                            template: """<p>Created game with _id: #{data._id}</p>"""
                            size: 'sm'
                        }
                    ).error( (err) ->
                        modal.open {
                            template: """<alert type="danger">Error: #{err}</alert>"""
                          
                        }
                    )
                scope.playGame = (_id) -> 
                    http({
                        method: 'POST'
                        url: "/api/games/#{_id}/join"
                    }).success( (data) ->
                        modal.open {
                            template: """<p>Joined game with _id: #{data._id}</p>"""
                            size: 'sm'
                        }
                        timeout ->
                            scope.$apply ->
                                scope.currentGameId = _id
                    ).error ((err) ->
                        modal.open {
                            template: """<alert type="danger">Error: #{err}</alert>"""
                            size: 'sm'
                        }
                    )

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
                destroyed = false
                scope.ctrl = 'GameListCtrl'
                games = []
                destroyed = false
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
                
                ajax = () ->
                    if not destroyed
                        log.debug "GameList poll"
                        http({
                            url: "/api/games"
                        }).success((data) ->
                            if not destroyed
                                log.debug("Updating game list to #{data.length} games")
                                games = data
                                scope.shownGames = games
                                scope.totalGames = games.length
                                # resliceGames()
                                setTimeout(ajax, 5000)
                        ).error((error) ->
                            log.error("Error in ajax poll for GameList" + error)
                        )

                ajax()
                scope.$watch 'currentPage', (newValue, oldValue) ->
                    log.debug("Current page: #{newValue} .. old: #{oldValue}")
                    resliceGames()
                scope.$on('$destroy', (->
                    destroyed = true
                ))   
           

    ])
    .controller('CurrentGameCtrl', ['$scope', '$log', '$http', 'StrategoFactory',
        class CurrentGameCtrl
            constructor: (scope, log, http, StrategoFactory) ->
                scope.ctrl = "CurrentGameCtrl"
                angular.element('canvas').ready () =>
                    @ctrl = new StrategoFactory.StrategoController(scope: scope, canvas: 'canvas', gameId: scope.currentGameId)
                    @ctrl.start()
                        .then ->
                            log.debug("CurrentGameCtrl started game")

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