QUnit.config.autostart = false;
QUnit.config.reorder = false
QUnit.config.testTimeout = 10 * 1000
window.ASSETS = "/assets/"
window.USER = {
    username: 'cbrahms'
    id: 'bla'
}

angular.injector(['ng', 'app.stratego']).invoke([
   '$q', '$log', 'StrategoFactory', 'StrategoActions', 'StrategoPieces', 
(Q, log, StrategoFactory, StrategoActions, StrategoPieces) ->    
    {StrategoAction, PlacePieceAction, MoveAction, AttackAction, ReplacePieceAction, CommitAction} = StrategoActions
    {StrategoPiece, RedPiece, BluePiece} = StrategoPieces

    board = new StrategoFactory.StrategoBoard({
        canvas: 'canvas'
        isRed: true    
    })
    board.init().then ->
        board.draw()
        test "Some setup", ->
            ok(board.isRed)
        asyncTest "Can remove pending layer",  -> 
            board.setRunning()
            ok(board.pendingLayer is null)
            board.draw()
            start()
        asyncTest "Can set pieces", ->
            count = board.redSideboard.getCount(StrategoPiece.LIEUTENANT)
            action = new PlacePieceAction({
                user: USER,
                x: 1
                y: 1
                piece: new RedPiece(StrategoPiece.LIEUTENANT)    
            })
            action.apply(board).then ->
                board.draw()
                ok(board.matrix[1][1].hasPiece())
                equal(board.redSideboard.getCount(StrategoPiece.LIEUTENANT), count-1)
                start()
        asyncTest "Set draggable", ->
            board.enableSideboard()
            board.enableDragging()
            notEqual(StrategoPiece.Empty, board.getPiece(1,1))
            ok(board.matrix[1][1].getPiece().image.draggable())
            start()
        asyncTest "setEmptyAndUpdateSideboard", ->
            value = board.getPiece(1,1).value
            count = board.redSideboard.getCount(value)
            board.setEmptyAndUpdateSideboard(1, 1)
            equal(board.redSideboard.getCount(value), count+1)
            equal(board.getPiece(1,1), StrategoPiece.Empty)
            board.draw()
            start()
        asyncTest "PlaceAllPieces", ->
            currentVal = 1
            defer = Q.defer()
            defer.resolve()
            promises = []
            [1..10].forEach (x) ->
                [1..4].forEach (y) ->
                   # promise = promise.then ->
                    log.debug("Currentval : " + currentVal)
                    log.debug("X: #{x} Y: #{y}")
                    count = board.redSideboard.getCount(currentVal)
                    if 0 == count then currentVal += 1
                    if currentVal < 13
                        piece = new RedPiece(currentVal)
                        action = new PlacePieceAction({
                            piece: piece
                            x: x
                            y: y 
                        })
                        promises.push action.apply(board)
                    #promise = promise.then ->
                    piece = new BluePiece(13)
                    action = new PlacePieceAction({
                        piece: piece
                        x: x
                        y: y + 6    
                    })
                    promises.push action.apply(board)


            Q.all(promises).then ->
                for val in [1..12]
                    ok(board.redSideboard.getCount(val) == 0)
                log.info("Test done")
                board.enableDragging()
                board.draw()
                start()
        asyncTest 'Test ReplacePieceAction when removing a piece', ->
            value = board.getPiece(1, 2).value

            action = new ReplacePieceAction {
                x: 1
                y: 1
                newX: 1
                newY: 2
                user: USER
            }
            ok(action.isLegal(board))
            action.apply(board).then ->
                equal(1, board.redSideboard.getCount(value))
                equal(StrategoPiece.Empty, board.getPiece(1,1))
                board.draw()
                start()

        asyncTest 'Test ReplacePieceAction when just moving', ->
            action = new ReplacePieceAction {
                x: 1
                y: 2
                newX: 1
                newY: 1
                user: USER
            }
            ok(action.isLegal(board))

            action.apply(board).then ->
                notEqual(StrategoPiece.Empty, board.getPiece(1,1))
                equal(0, board.redSideboard.getCount(board.getPiece(1,1).value))
                equal(StrategoPiece.Empty, board.getPiece(1,2))
                board.draw()
                start()

        asyncTest 'Put back the 2', ->
            action = new PlacePieceAction {
                piece: new RedPiece(2)
                x: 1
                y: 2
                user: USER
            }
            action.apply(board).then ->
                equal(2, board.getPiece(1,2).value)
                board.draw()
                start()
        asyncTest 'ReplacePieceAction on blue side', ->
            action = new ReplacePieceAction {
                x: 1
                y: 10
                newX: 1
                newY: 9
                user: {
                    username: 'blue'
                }
            }
            action.apply(board)
                .then ->
                    equal(StrategoPiece.Empty, board.getPiece(1,10))
                    action = new PlacePieceAction {
                        piece: new BluePiece(13)
                        x: 1
                        y: 10
                    }
                    action.apply(board)
                .then ->
                    ok("Worked")
                    start()


        asyncTest 'CommitPlayer ' + USER.username, ->
            action = new CommitAction {
                user: {
                    username: USER.username
                }
            }
            action.apply(board)
                .then ->
                    ok(board.redPlayerReady)
                    action = new CommitAction {
                        user: {
                            username: 'blue'
                        }
                    }
                    promise =  action.apply(board)
                    promise
                .then ->
                    equal(board.phase, "RUNNING")
                    start()

        asyncTest 'Move Red Scout 3', ->
            action = new MoveAction {
                x: 1
                y: 4
                newX: 1
                newY: 6
            }
            ok(action.isLegal(board))
            action.apply(board).then ->
                equal(board.getPiece(1, 4), StrategoPiece.Empty)
                board.draw()
                start()
            
        QUnit.start()

])

