angular.module('app.stratego.square', ['app.stratego.functions', 'app.stratego.pieces'])
    .factory('StrategoSquare', ['$log', 'StrategoFunctions', 'StrategoPieces',
(log, StrategoFunctions, StrategoPieces) ->
    {SQUARE_WIDTH, SQUARE_HEIGHT, squareToXy} = StrategoFunctions
    {StrategoPiece, RedPiece, BluePiece} = StrategoPieces
    class StrategoSquare extends EventEmitter
        constructor: ({x, y, layer, boardOffset, emitter, isRed}) ->
            @x = x
            @y = y
            if !x? then throw "StrategoSquare x null"
            if !y? then throw "StrategoSquare y null"
            if !layer? then throw "StrategoSquare layer null"
            if !boardOffset? then throw "StrategoSquare boardOffset null"
            if !emitter? then throw "StrategoSquare emitter null"
            if !isRed? then throw "StrategoSquare isRed null"

            {x,y} = squareToXy x, y
            @layer = layer
            @layerX = x + boardOffset
            @layerY = y
            @emitter = emitter
            @rect = new Kinetic.Rect {
                x: @layerX
                y: @layerY
                width: SQUARE_WIDTH
                height: SQUARE_HEIGHT
                stroke: 'black'
                strokeWidth: 5
            }
            @piece = null
            @draggable = false
            @isRed = isRed

            layer.add @rect
        toString: =>
            "StrategoSquare[#{@x}/#{@y}, Piece: #{@getPiece()}, Draggable: #{@draggable} }]";
        getPiece: =>
            if (@piece)
                @piece
            else
                StrategoPiece.Empty
        setPiece: (piece) =>
            if !piece? then throw "#{@} setPiece piece null"
            if piece not instanceof StrategoPiece  then throw "#{@} setPiece #{piece} not stratego piece"
            log.debug("#{@} setPiece: #{piece}")
            @piece = piece
            piece.setSquare(@)
            promise = @piece.addToLayer {
                layer: @layer
                x: @layerX
                y: @layerY
                emitter: @emitter
            }
            promise
                .then ()=>
                    if @piece.testColor(@isRed)
                        if @draggable 
                            log.debug("#{@} setting #{piece} to draggable")
                            @piece.draggableOn() 
                        else
                            log.debug("#{@} setting #{piece} to not drabble")
                            @piece.draggableOff()
        isOtherUser: (piece) =>
            if piece instanceof RedPiece and @hasBluePiece() then return true
            if piece instanceof BluePiece and @hasRedPiece() then return true
            false
        hasRedPiece: ->
            if @piece and @piece instanceof RedPiece
                true
            else
                false
        hasBluePiece: ->
            if @piece and @piece instanceof BluePiece
                true
            else
                false
        draggableOn: ->
            @draggable = true
            if @piece and @piece.testColor(@isRed) then @piece.draggableOn()
        draggableOff: ->
            @draggable = false
            if @piece then @piece.draggableOff()
        hasPiece: ->
            if @piece then true
            else false
        setEmpty: ->
            log.debug("#{@} setEmpty() piece: #{@piece}")
            @piece = null
        isEmpty: ->
            not @piece

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