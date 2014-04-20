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
                log.debug("Initializing from #{angular.toJson(game)}")
                if (game.redPlayer) then @board.setRedPlayer(game.redPlayer)
                if (game.bluePlayer) then @board.setBluePlayer(game.bluePlayer)

            else
                log.debug("Initializing from #{angular.toJson(game)}")
                board.setState(game.state)
                @board.setRedPlayer(game.redPlayer)
                @board.setBluePlayer(game.bluePlayer)

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