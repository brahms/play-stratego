angular.module('app.stratego.square', [])
    .factory('StrategoSquare', ['$log', 
(log) ->
    class StrategoSquare
        constructor: (@x, @y, @game, @layer) ->
            {x,y} = squareToXy @x, @y
            log.debug("#{@x}/#{@y} -> #{x}/#{y}")
            log.debug("#{x+@game.offsetx}")
            @rect = new Kinetic.Rect {
                x: x+@game.offsetx,
                y: y
                width: SQUARE_WIDTH
                height: SQUARE_HEIGHT
                stroke: 'black'
                strokeWidth: 5
            }

            @layer.add @rect
            @rect.on 'mouseover', @mouseover
            @rect.on 'mouseout', @mouseout
            @rect.on 'click', @click
        mouseover: =>
            log.debug "Mosueover: #{@}"
        mouseout: =>
            log.debug "Mouseout: #{@}"
        click: =>
            log.debug "Click #{@}"
        toString: =>
            piece = @game.getPiece(@x, @y)
            return "StrategoSquare[#{@x}/#{@y}, Piece: #{piece}]";
])