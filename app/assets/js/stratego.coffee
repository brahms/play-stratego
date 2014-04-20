angular.module('app.stratego', [
    'app.stratego.pieces', 
    'app.stratego.board', 
    'app.stratego.square',
    'app.stratego.functions',
    'app.stratego.actions',
    'app.stratego.controller',
    'app.stratego.sideboards',
    'app.stratego.invoker'
]).factory('StrategoFactory', [
    '$log', 
    '$http', 
    'StrategoPieces', 
    'StrategoBoard', 
    'StrategoSquare',
    'StrategoActions',
    'StrategoController',
(log, http, StrategoPieces, StrategoBoard, StrategoSquare, StrategoActions, StrategoController) ->


    return {
            StrategoController: StrategoController
            StrategoBoard: StrategoBoard
        }

])