angular.module('app.stratego.controller', ['app.stratego.actions', 'app.stratego.invoker', 'app.stratego.pieces'])
    .factory('StrategoController', ['$timeout', '$log', '$http', '$q', 'StrategoActions', 'StrategoInvoker', 'StrategoPieces', 'StrategoBoard',
(timeout, log, http, Q, StrategoActions, StrategoInvoker, StrategoPieces, StrategoBoard) ->
    {StrategoAction, PlacePieceAction, MoveAction, AttackAction, ReplacePieceAction, CommitAction} = StrategoActions
    {StrategoPiece, RedPiece, BluePiece} = StrategoPieces
    class StrategoController
        constructor: ({gameId, canvas, scope}) ->
            if !gameId? then throw "StrategoController Game _id is null"
            if !canvas? then throw "StrategoController Canvas is null"
            if !scope? then throw "StrategoController Scope is null"
            @gameId = gameId
            log.debug('StrategoController constructor');
            @invoker = new StrategoInvoker(gameId: @gameId, user: USER)
            @invoker.on('init', @_onInit)
            @invoker.on('data', @_onData)
            @canvas = canvas
            @board = null
            @scope = scope
            scope.showCommit = true
            scope.disableCommit = true
            scope.commit = @commit
            scope.showCancel = true
            scope.cancel = @cancel
            @actionQueue = []
        start: ->
            defer = Q.defer()
            @invoker.start()
                .then () =>
                    defer.resolve()
                .catch () =>
                    defer.reject()
            defer.promise

        _onData: (actions) =>
            log.debug("On Data: #{angular.toJson(actions)}")
            actions.forEach (action) =>
                action = StrategoAction.fromJson(action)
                log.debug("Created action: #{action}")
                @actionQueue.push(action)

            @_invokeAnyAction()

        _onInit: (game) =>
            log.debug("On init: #{angular.toJson(game)}")
            if (game.state == 'PENDING')
                log.debug("Game still in pending state")
                @_createOrIgnoreBoard(game)
            else if (game.state == 'RUNNING')
                timeout( () =>
                    log.debug("Applying scope")
                    @scope.$apply () =>
                        log.debug("Setting show cancel to false")
                        @scope.showCancel = false
                )
                @_createOrIgnoreBoard(game).then () =>
                    log.debug("Initializing from #{angular.toJson(game)}")
                    @board.setRunning()
                    actions = (StrategoAction.fromJson(jsonAction) for jsonAction in  game.actionList)
                    d = Q.defer()
                    d.resolve()
                    promise = d.promise
                    actions.forEach (action) =>
                        promise = promise.then(()=>
                            log.debug("#{@} applying action: #{action}")
                            action.apply(@board)
                        )
                    promise.finally () =>
                        log.debug("Finished applying actions")
                        @board.setPhase(game.phase)
                        switch game.phase
                            when "PLACE_PIECES" 
                                log.debug("Enable sideboard")
                                @board.enableSideboard()
                            else 
                                timeout () =>
                                    @scope.$apply () =>
                                        log.debug("Setting show commit to false")
                                        @scope.showCommit = false
                        @board.enableDragging()
                        @board.draw()
                        @invoker.startGrabbingActions()
                        @_invokeAnyAction()

        _invokeAnyAction: () =>
            if @actionQueue.length > 0
                action = @actionQueue.shift()
                log.debug("Invoking action on board: #{action}")
                action.apply(@board).finally () =>
                    @board.draw()
                    timeout @_invokeAnyAction

        _onPieceMoved: (piece, pos) =>
            log.debug("#{@} _onPieceMoved: #{piece}")
            if !piece.getSquare()? then throw "Error: _onPieceMoved #{piece} has no current square"
            square = @board.getSquareForLayerPoint({
                layerX: pos.x
                layerY: pos.y      
            })
            if square
                action = null
                switch(@board.phase)
                    when 'PLACE_PIECES'
                        action = new ReplacePieceAction {
                            user: USER
                            x: piece.getSquare().x
                            y: piece.getSquare().y
                            newX: square.x
                            newY: square.y
                        }
                    when 'RUNNING'
                        if (square.isOtherUser(piece))
                            action = new AttackAction {
                                x: piece.getSquare().x
                                y: piece.getSquare().y
                                newX: square.x
                                newY: square.y
                                user: USER
                            }
                        else if (square.isEmpty())
                            action = new MoveAction {
                                x: piece.getSquare().x
                                y: piece.getSquare().y
                                newX: square.x
                                newY: square.y
                                user: USER
                            }
                        else
                            log.debug("Cannot create action for #{piece} to move to #{square}")
                if action and action.isLegal(@board)
                    log.debug("#{@} creating action: #{action}")
                    @invoker.invoke(action)
                else
                    log.debug("Invalid action")
                    piece.reset()
            else
                log.debug("Invalid move")
                piece.reset()

        commit: =>
            log.debug("#{@} commit called")
        cancel: =>
            log.debug("#{@} Cancel called")
            @invoker.stop()
            http({
                method: 'POST'
                url: '/api/games/cancel'
            }).finally(() =>
                timeout( () =>
                    @scope.$apply () => 
                        @scope.currentGameId = null
                        window.location = "/app"
                )
            )

        _onPiecePlaced: (piece, pos) =>
            log.debug("#{@} _onPiecePlaced: #{piece}")
            square = @board.getSquareForLayerPoint({
                layerX: pos.x
                layerY: pos.y
            })
            if square
                action = new PlacePieceAction {
                    x: square.x
                    y: square.y
                    piece: if @board.isRed then new RedPiece(piece.value) else new BluePiece(piece.value)
                }
                log.debug("#{@} creating action: #{action}")
                if action.isLegal(@board) then @invoker.invoke(action)


            piece.reset()


        _createOrIgnoreBoard: (game) -> 
            log.debug("_createOrIgnoreBoard: " + @board)
            d = Q.defer()
            if @board is null
                if (game.redPlayer.username is USER.username) 
                    log.debug("Creating board for red player")
                    @board = new StrategoBoard(canvas: @canvas, isRed: true)
                else 
                    log.debug("Creating board for blue player")
                    @board = new StrategoBoard(canvas: @canvas, isRed: false)
                @board.init().finally () =>
                        @board.draw()
                        @board.on 'place', @_onPiecePlaced
                        @board.on 'move', @_onPieceMoved
                        d.resolve()
            else
                d.resolve()
            d.promise
        toString: ->
            "StrategoController[#{@gameId}]"
    
])