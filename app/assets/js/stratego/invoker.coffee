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
            @phase = 'init'

            "#{@} constructor"
        setPhase: (phase) ->
            log.debug("#{@} setPhase #{phase}")
            @phase = phase
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
                lastActionId: 0
            }).success((data)=>
                if (data.state == 'PENDING') then 
                log.debug("#{@} got game initial state, starting interval")
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

        _updateActionId: (data) ->
            log.debug("#{@} updateActionId")

            if data.actionList?.length > 0
                @lastActionId = data.actionList[data.actionList.length-1].actionId

            log.debug("#{@} Updated to : #{@lastActionId}")
        _onInterval: =>
            log.debug("#{@} onInterval, phase = #{@phase}")
            if @phase == 'init'
                http({
                    url: "/api/games/#{@gameId}"
                    data: {
                        lastActionId : @lastActionId
                    }
                }).success( (data) =>
                    log.debug("#{@} Got more game state")
                    @_updateActionId(data)
                    @emit('init', data)
                ).error( () =>
                    log.error("#{@} Error getting data in onInterval")
                ).finally( () =>
                    $window.setTimeout(@_onInterval, 5 * 1000)
                )
            else if @phase == 'running'
                http({
                    url: "/api/games/#{@gameId}/actions"
                    data: {
                        lastActionId : @lastActionId
                    }
                }).success( (data) =>
                    log.debug("#{@} Got more game state")
                    @_updateActionId(data)
                    @emit('data', data)
                ).error( () =>
                    log.error("#{@} Error getting data in onInterval")
                ).finally( () =>
                    #$window.setTimeout(@_onInterval, 1 * 1000)
                )

        toString: ->
            "StrategoInvoker[gameId: #{@gameId} user: #{@user.username}]"

])