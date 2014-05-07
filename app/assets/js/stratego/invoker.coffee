angular.module('app.stratego.invoker', [])
.factory('StrategoInvoker', [ '$log', '$http', '$window', '$q'
(log, http, $window, Q) ->
    class StrategoInvoker extends EventEmitter
        constructor: ({gameId, user}) ->
            if !gameId? then throw "StrategoInvoker gameId null"
            if !user? then throw "StrategoInvoker user null"

            @gameId = gameId
            @user = user
            @lastActionId = 0
            @invokerPhase = 'init'

            "#{@} constructor"
        startGrabbingActions: () ->
            @invokerPhase = 'actions'
            @_onInterval()
        invoke: (action) ->
            defer = Q.defer()
            defer.resolve()
            log.debug("#{@} invoking #{action}")
            json = action.toJson()
            log.debug("jsonified: #{angular.toJson(json)}")
            http({
                url: "/api/games/#{@gameId}/action"
                method: 'POST'
                data: json
            }).success(() =>
                log.debug("Invoking worked")
                defer.resolve()
            ).error(() =>
                log.debug("Invoking failed")
                defer.reject()
            )
            defer.promise
        start: -> @_onInterval()
        stop: ->
            @_onInterval = () ->

        _updateActionId: (data) ->

            if data.length > 0
                @lastActionId = data[data.length-1].actionId

            log.debug("#{@} Updated action id to : #{@lastActionId}")
        _onInterval: =>
            defer = Q.defer()
            if @invokerPhase == 'init'
                http({
                    url: "/api/games/#{@gameId}"
                    data: {
                        lastActionId : @lastActionId
                    }
                }).success( (game) =>
                    log.debug("#{@} Got game state")
                    if (game.state == "PENDING") 
                        log.debug("INVOKER - Game still in pending state")
                        $window.setTimeout(@_onInterval, 5 * 1000)
                    else 
                        @invokerPhase = 'actions'
                        log.debug("INVOKER - game is in #{game.state} state")
                        @emit('init', game)
                    defer.resolve()
                ).error( () =>
                    log.error("#{@} Error getting game in onInterval")
                    defer.reject()
                )
            else if @invokerPhase == 'actions'
                http({
                    url: "/api/games/#{@gameId}/actions?lastActionId=#{@lastActionId}"
                    data: {
                        lastActionId : @lastActionId
                    }
                }).success( (data) =>
                    if (data.length)
                        log.debug("#{@} Got more game state")
                        @_updateActionId(data)
                        @emit('data', data)
                    defer.resolve()

                ).error( () =>
                    log.error("#{@} Error getting data in onInterval")
                    defer.reject()
                ).finally( () =>
                    $window.setTimeout(@_onInterval, 1 * 1000)
                )
            defer.promise.then () =>
                "#{@} done loading"

        toString: ->
            "StrategoInvoker[gameId: #{@gameId} user: #{@user.username}]"

])