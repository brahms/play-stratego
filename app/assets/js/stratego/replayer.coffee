angular.module('app.stratego.replayer', ['app.stratego.actions', 'app.stratego.pieces'])
    .factory('StrategoReplayer', ['$timeout', 
        '$log', 
        '$http', 
        '$q', 
        'StrategoActions', 
        'StrategoPieces', 
        'StrategoBoard',
(timeout, log, http, Q, StrategoActions, StrategoPieces, StrategoBoard) ->
    {StrategoAction, PlacePieceAction, MoveAction, AttackAction, ReplacePieceAction, CommitAction} = StrategoActions
    {StrategoPiece, RedPiece, BluePiece} = StrategoPieces
    class StrategoReplayer
        constructor: (gameId) ->
            if !gameId? then throw "#{@} gameId"
            @gameId = gameId
            @actionQueue = []
        toString: -> "StrategoReplayer[#{@gameId}]"
        start: ->
            @defer = Q.defer()
            http({
                url: "/api/games/#{@gameId}"
            }).success((game) =>
                log.info("Received game state: #{angular.toJson(game)}")
                @board = new StrategoBoard({
                    isRed: true
                    canvas: 'replayCanvas'
                })
                @board.init().then(() =>
                    @board.draw()
                    log.debug("Creating action list")
                    for json in game.actionList
                        @actionQueue.push(StrategoAction.fromJson(json))

                    log.debug("creating list of size: #{@actionQueue.length}")
                    @board.setRunning()
                    @board.enableAnimations()
                    @board.draw()
                    @_applyNextAction()

                )
            ).error((err) =>
                log.error("Error receiving game state")
                @defer.reject()
            )


            @defer.promise


        _applyNextAction: () =>
            if @actionQueue.length > 0
                action = @actionQueue.shift()
                log.debug("Invoking action on board: #{action}")
                action.apply(@board).finally () =>
                    @board.draw()
                    timeout @_applyNextAction

])

  