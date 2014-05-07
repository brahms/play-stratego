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

            "#{@} constructor"
        startGrabbingActions: () ->
            @_actionInterval()
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
        start: -> 
            defer = Q.defer()
            http({
                url: "/api/games/#{@gameId}"
                data: {
                    lastActionId : @lastActionId
                }
            }).success( (game) =>
                log.debug("#{@} Got game state")
                if (game.state == "PENDING") 
                    @gamePhase = game.state
                    $window.setTimeout(@_pendingInterval, 5 * 1000)
                else 
                    @gamePhase = game.state
                    $window.setTimeout(@_actionInterval, 5 * 1000)
                    @_updateActionId(game.actionList)
                defer.resolve(game)
            ).error( () =>
                log.error("#{@} Error getting game in onInterval")
                defer.reject()
            )
            defer.promise
        stop: ->
            @_onInterval = () ->

        _updateActionId: (data) ->

            if data.length > 0
                @lastActionId = data[data.length-1].actionId

            log.debug("#{@} Updated action id to : #{@lastActionId}")

        _pendingInterval: =>
            http({
                url: "/api/games/#{@gameId}"
            }).success( (game) =>
                log.debug("#{@} Got game state")
                if (game.state == "PENDING") 
                    log.debug("INVOKER - Game still in pending state")
                    $window.setTimeout(@_pendingInterval, 5 * 1000)
                else
                    @emit('running', game)
            )

        _actionInterval: =>
            http({
                url: "/api/games/#{@gameId}/actions?lastActionId=#{@lastActionId}"
                data: {
                    lastActionId : @lastActionId
                }
            }).success( (actions) =>
                if (actions.length)
                    @_updateActionId(actions)
                    log.debug("#{@} retrieved #{actions.length} actions")
                    @emit('actions', actions)
            ).error( () =>
                log.error("#{@} Error getting data in onInterval")
            ).finally( () =>
                $window.setTimeout(@_actionInterval, 1 * 1000)
            )

        toString: ->
            "StrategoInvoker[gameId: #{@gameId} user: #{@user.username}]"

])