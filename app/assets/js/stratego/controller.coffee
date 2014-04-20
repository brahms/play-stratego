angular.module('app.stratego.controller', ['app.stratego.actions', 'app.stratego.invoker', 'app.stratego.pieces'])
    .factory('StrategoController', ['$log', '$http', '$q', 'StrategoActions', 'StrategoInvoker', 'StrategoPieces',
(log, http, Q, StrategoActions, StrategoInvoker, StrategoPieces) ->
    {StrategoAction, PlacePieceAction, MoveAction, AttackAction, ReplacePieceAction, CommitAction} = StrategoActions
    {StrategoPiece, RedPiece, BluePiece} = StrategoPieces
    class StrategoController
        constructor: ({gameId, board}) ->
            if !gameId? then throw "StrategoController Game id is null"
            if !board? then throw "StrategoController Board is null"
            @gameId = gameId
            @board = board
            log.debug('StrategoController constructor');
            @invoker = new StrategoInvoker(gameId: @gameId, user: USER)
            @invoker.on('init', @_onInit)
            @invoker.on('data', @_onData)

            @board.on 'place', @_onPiecePlaced
            @board.on 'move', @_onPieceMoved
        start: ->
            defer = Q.defer()
            @board.promise
                .then () =>
                    @invoker.start()
                .then () =>
                    defer.resolve()
                .catch () =>
                    defer.reject()
            defer.promise

        _onData: =>

        _onInit: (game) =>
            if (game.state == 'PENDING')
                log.debug("Game still in pending state")
            else if (game.state == 'RUNNING')
                @invoker.setPhase('running')
                log.debug("Initializing from #{angular.toJson(game)}")
                @board.setRunning()
                actions = (StrategoAction.fromJson(jsonAction) for jsonAction in  game.actionList)
                promises = []
                actions.forEach (action) =>
                    log.debug("#{@} applying action: #{action}")
                    promises.push(action.apply(@board))
                Q.all(promises)
                    .finally () =>
                        log.debug("Finished applying actions")
                        @board.setPhase(game.phase)
                        switch game.phase
                            when "PLACE_PIECES" 
                                @board.enableSideboard()

                        @board.enableDragging()
                        @board.draw()

        _onPieceMoved: (piece, pos) =>
            log.debug("#{@} _onPieceMoved: #{piece}")
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



        toString: ->
            "StrategoController[#{@gameId}]"
    
])