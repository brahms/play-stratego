angular.module('app.stratego.controller', ['app.stratego.actions'])
    .factory('StrategoController', ['$log', '$http', 'StrategoActions'
(log, http, StrategoActions) ->
    class StrategoController
        constructor: (@gameId) ->
            log.debug('StrategoController constructor');
        invoke: (action) ->
            log.debug('StrategoController invoke');
])