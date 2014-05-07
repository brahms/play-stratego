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
            }).success((data)=>
                @_updateActionId(data)
                @emit('init', data)
                $window.setTimeout(@_onInterval, 1 * 1000)
                defer.resolve()
            ).error(()=>
                log.debug("#{@} unable to get game state")
                defer.reject()
            )
            defer.promise.then () =>
                "#{@} done loading"
        stop: ->
            @_onInterval = () ->

        _updateActionId: (data) ->
            log.debug("#{@} updateActionId to : #{data.actionId}")

            if data.actionList?.length > 0
                @lastActionId = data.actionList[data.actionList.length-1].actionId

            log.debug("#{@} Updated to : #{@lastActionId}")
        _onInterval: =>
            log.debug("#{@} onInterval, invokerPhase = #{@invokerPhase}")
            if @invokerPhase == 'init'
                http({
                    url: "/api/games/#{@gameId}"
                    data: {
                        lastActionId : @lastActionId
                    }
                }).success( (game) =>
                    log.debug("#{@} Got more game state")
                    if (game.state == "PENDING") 
                        log.debug("INVOKER - Game still in pending state")
                        return
                    else 
                        log.debug("INVOKER - game is in #{game.state} state")
                    @emit('init', game)
                ).error( () =>
                    log.error("#{@} Error getting game in onInterval")
                ).finally( () =>
                    $window.setTimeout(@_onInterval, 5 * 1000)
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
                    else
                        log.debug("#{@} No new actions")
                ).error( () =>
                    log.error("#{@} Error getting data in onInterval")
                ).finally( () =>
                    $window.setTimeout(@_onInterval, 1 * 5000)
                )

        toString: ->
            "StrategoInvoker[gameId: #{@gameId} user: #{@user.username}]"

])