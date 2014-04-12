angular.module('app.stratego.square', ['app.stratego.functions'])
    .factory('StrategoSquare', ['$log', 'StrategoFunctions', 
(log, StrategoFunctions) ->
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions
    class StrategoSquare
        constructor: (@x, @y, @board, @layer) ->
            {x,y} = squareToXy @x, @y
            log.debug("#{@x}/#{@y} -> #{x}/#{y}")
            log.debug("#{x+@board.offsetx}")
            @layerX = x + @board.offsetx
            @layerY = y
            @rect = new Kinetic.Rect {
                x: @layerX,
                y: @layerY
                width: SQUARE_WIDTH
                height: SQUARE_HEIGHT
                stroke: 'black'
                strokeWidth: 5
            }
            @piece = null

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
            "StrategoSquare[#{@x}/#{@y}, Piece: #{@getPiece()}]";
        getPiece: =>
            if (@piece)
                @piece
            else
                0
        onPieceDropped: (piece) ->
            log.debug("onPieceDropped: piece: #{piece} this: #{@}")
        setPiece: (piece) =>
            @piece = piece
            log.debug("#{@toString()} Adding piece: #{@getPiece()}")
            @piece.setSquare(@)
        initPiece: (@piece) =>
            @setPiece(@piece)
            @piece.addToLayer(@layer)
        intersects: (layerX, layerY) ->
            x1 = @rect.x()
            x2 = x1 + @rect.width()
            y1 = @rect.y()
            y2 = y1+@rect.height()

            if (layerX < x1 or layerX > x2)
                return false
            if (layerY < y1 or layerY > y2) 
                return false

            log.debug("intersects #{@} #{layerX}/#{layerY}")
            return true;

])