angular.module('app.stratego', [
    'app.stratego.pieces', 
    'app.stratego.board', 
    'app.stratego.square',
    'app.stratego.functions',
    'app.stratego.actions',
    'app.stratego.controller',
    'app.stratego.sideboards',
    'app.stratego.invoker',
    'app.stratego.replayer'
]).factory('StrategoFactory', [
    '$log', 
    '$http', 
    'StrategoPieces', 
    'StrategoBoard', 
    'StrategoSquare',
    'StrategoActions',
    'StrategoController',
    'StrategoReplayer',
(log, http, StrategoPieces, StrategoBoard, StrategoSquare, StrategoActions, StrategoController, StrategoReplayer) ->


    return {
            StrategoController: StrategoController
            StrategoReplayer: StrategoReplayer
        }

])