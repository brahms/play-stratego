angular.module('app.stratego.actions', ['app.stratego.pieces'])
    .factory('StrategoActions', ['$q', '$log', 'StrategoPieces',
(Q, log, StrategoPieces) ->
    {StrategoPiece} = StrategoPieces
    class StrategoAction
        constructor: ->
        isLegal: (board) ->
            throw "isLegal Not implemented"
        apply: (board) ->
            throw "apply Not Implemented"
        invoke: (board) -> @apply(board)
        toJson: (json) ->
            throw "toJson Not implemented"
        fromJson: (json) ->
            throw "fromJson Not implemented"

    StrategoAction.fromJson = (json) ->
        action = null
        switch json.type
            when 'MoveAction' then action = new MoveAction({})
            when 'PlacePieceAction' then action = new PlacePieceAction({})
            when 'ReplacePieceAction' then action = new ReplacePieceAction({})
            when 'AttackAction' then action = new AttackAction({})
            when 'CommitAction' then action = new CommitAction({})
        action.fromJson(json)
        action

    class MoveAction extends StrategoAction
        constructor: ({x, y, newX, newY}) ->
            @x = x
            @y = y
            @newX = newX
            @newY = newY
        isLegal: (board) ->
            if board.isOutOfBounds(@newX, @newY) then return false
            piece = board.getPiece(@x, @y)
            maxDist = 1
            if (piece.value is StrategoPiece.SCOUT) then maxDist = 10
            if (board.getDistance(@x, @y, @newX, @newY) > maxDist) then return false
            if (board.isThroughLakes(@x, @y, @newX, @newY)) then return false
        apply: (board) ->
            if (board.animationsEnabled)
                board.animatedMove(@x, @y, @newX, @newY)
            else 
                board.movePiece(@x, @y, @newX, @newY)
        toJson: (json) -> {
                type: 'MoveAction'
                x: @x
                y: @y
                newX: @newX
                newY: @newY
            }
        fromJson: (json) ->
            @x = json.x
            @y = json.y
            @newX = json.newX
            @newY = json.newY
            @user = json.user
        toString: ->
            "MoveAction[user: #{@user}, x: #{@x}, y: #{@y} newX: #{@newX}, newY: #{@newY}"

    class AttackAction extends StrategoAction
        constructor: ({user, x, y, newX, newY, attacker, defender, result}) ->
            @user = user
            @x = x
            @y = y
            @newX = newX
            @newY = newY
            @attacker = attacker
            @defender = defender
            @result = result

        isLegal: (board) ->
            if (board.isOutOfBounds(@newX, @newY)) then return false
            piece = board.getPiece(@x, @y)
            if piece.value == 13 then return false
            defender = board.getPiece(@newX, @newY)
            if defender.value != 13 then return false

            true
        apply: (board) ->
            if board.animationsEnabled
                board.animatedKill(@)
            else
                board.killPiece(@)
        toJson: (json) -> {
                type: 'AttackAction'
                x: @x
                y: @y
                newX: @newX
                newY: @newY
        }
        fromJson: (json) ->
            @x = json.x
            @y = json.y
            @newX = json.newX
            @newY = json.newY
            @user = json.user
            @attacker = StrategoPiece.fromJson(json.attacker)
            @defender = StrategoPiece.fromJson(json.defender)
            @result = json.result
        toString: ->
            "AttackAction[user: #{@user}, \
            x: #{@x}, \
            y: #{@y} \
            newX: #{@newX}, \
            newY: #{@newY}, \
            attacker: #{@attacker}, \
            defender: #{@defender}, \
            result: #{@result}]"
    class PlacePieceAction extends StrategoAction
        constructor: ({user, x, y, piece}) ->
            @user = user
            @x = x
            @y = y
            @piece = piece
        isLegal: (board) ->
            log.debug("PlacePieceAction.isLegal")
            if board.phase != 'PLACE_PIECES'
                return false

            log.debug("PlacePieceAction.isLegal return true")

            true
        apply: (board) ->
            board.setEmptyAndUpdateSideboard(@x, @y)

            promise = board.placePieceAndUpdateSideboard(@piece, @x, @y)
            promise
        toJson: (json) -> {
            type: 'PlacePieceAction'
            x: @x
            y: @y
            piece: @piece.toJson()
        }
        fromJson: (json) -> 
            @x = json.x
            @y = json.y
            @piece = StrategoPiece.fromJson(json.piece)
            @user = json.user
        toString: ->
            "PlacePieceAction[user: #{@user}, x:#{@x} y:#{@y} piece:#{@piece}]"

    class ReplacePieceAction extends StrategoAction
        constructor: ({user, x, y, newX, newY}) ->
            @user = user
            @x = x
            @y = y
            @newX = newX
            @newY = newY
        isLegal: (board) ->
            not board.isOutOfBounds(@newX, @newY) and board.isOnUsersSide(@newX, @newY)
        apply: (board) ->
            if !@x? then throw "ReplacePieceAction x null"
            if !@y? then throw "ReplacePieceAction y null"
            if !@newX? then throw "ReplacePieceAction newX null"
            if !@newY? then throw "ReplacePieceAction newY null"
            if !@user? then throw "ReplacePieceAction user null"
            defer = Q.defer()
            defer.resolve()
            board.setEmptyAndUpdateSideboard(@newX, @newY)
            board.movePiece(@x, @y, @newX, @newY)
            defer.promise

        toJson: (json) -> {
            type: 'ReplacePieceAction'
            x: @x
            y: @y
            newX: @newX
            newY: @newY
        }
        fromJson: (json) -> 
            @x = json.x
            @y = json.y
            @newX = json.newX
            @newY = json.newY
        toString: ->
            "ReplacePieceAction[user: #{@user}, x:#{@x} y:#{@y} newX: #{@newX}, newY: #{@newY}]"


    class CommitAction extends StrategoAction
        constructor: ({user})->
            @user = user
        isLegal: (board) ->
            board.phase == "PHASE_PIECES"
        apply: (board) ->
            if USER.username == @user.username then board.commitThisPlayer()
            else board.commitOtherPlayer()

            d = Q.defer(); d.resolve()

            d.promise
        toJson: (json) -> {
            user: @user
        }
        fromJson: (json) ->
            @user = json.user

        toString: ->
            "CommitAction[user: #{@user}]"

    {
        StrategoAction: StrategoAction
        MoveAction: MoveAction
        AttackAction: AttackAction
        ReplacePieceAction: ReplacePieceAction
        PlacePieceAction: PlacePieceAction
        CommitAction: CommitAction
    }

])