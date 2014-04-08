angular.module('app.stratego', [
    'app.stratego.piece', 
    'app.stratego.board', 
    'app.stratego.square'
]).factory('StrategoFactory', [
    '$log', 
    '$http', 
    'StrategoPiece', 
    'StrategoBoard', 
    'StrategoSquare',
(log, http, StrategoPiece, StrategoBoard, StrategoSquare) ->
    class StrategoAction
        constructor: ->
        isLegal: ->
            throw "isLegal Not implemented"
        apply: ->
            throw "apply Not Implemented"
        toJson: ->
            throw "toJson Not implemented"

    StrategoAction.fromJson = (obj) ->
        ""

    class MoveAction extends StrategoAction
        constructor: ->

    class AttackAction extends StrategoAction
    class PlacePieceAction extends StrategoAction
    class ReplacePieceAction extends StrategoAction

    class StrategoModel
        constructor: (@gameId, @user) ->
            @boardMatrix = [
                    [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0,-1,-1, 0, 0,-1,-1, 0, 0,-1]
                    [-1, 0, 0,-1,-1, 0, 0,-1,-1, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,-1]
                    [-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]
                ]
            @currentPlayer = @user


    class StrategoView
        constructor: (@model, @ctrl) ->
          

    class StrategoController
        constructor: (@model) ->
        invoke: (action) ->
            log('test');

    return {
            StrategoModel: StrategoModel
            StrategoController: StrategoController
            StrategoView: StrategoView
        }

])