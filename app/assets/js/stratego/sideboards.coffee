angular.module('app.stratego.sideboards', ['app.stratego.actions', 
    'app.stratego.functions',
    'app.stratego.pieces'])
    .factory('StrategoSideboards', ['$log', 'StrategoFunctions',  'StrategoPieces'
(log, StrategoFunctions, StrategoPieces) ->

    {StrategoPiece} = StrategoPieces
    START_X = 100
    END_X  = 530
    RED_Y = 560
    BLUE_Y = 10
    TEXT_OFFSET_X = 20
    TEXT_OFFSET_Y = 20
    getCenter =  (image) -> {
        x: (image.x() + (image.width()/2))
        y: (image.y() + (image.height()/2))
    }
    class StrategoSideboard
        constructor: (@color, @isUser, @board) ->
        addToLayer: (layer) ->
            @squares = {}

            [startX, startY] = if (@color=='r') then [START_X, RED_Y] else [START_X, BLUE_Y]
            width = Math.floor((END_X-START_X)/12)
            height = width
            log.debug("StartX: #{startX} StartY: #{startY}")

            [StrategoPiece.MINVAL..StrategoPiece.MAXVAL].forEach (value) =>
                x = startX + width*(value-1)
                y = startY
                imgElement = new Image()
                imgElement.src = @getImageUrl(value)
                imgElement.onload = => 
                    log.debug("onload: #{@getImageUrl(value)}")
                    log.debug("Adding rect to #{x}/#{y} of height: #{height} width: #{width}")
                    rect = new Kinetic.Rect {
                        x: x
                        y: y
                        width: width
                        height: height
                        stroke: 'black'
                        strokeWidth: 5
                    }

                    if (@isUser)
                        draggableImage = new Kinetic.Image {
                            image: imgElement
                            draggable: true
                            x: x
                            y: y
                            width: width
                            height: height
                        }
                        draggableImage.on('dragend', =>
                            pos = getCenter(draggableImage)
                            boardSquare = @board.getSquareForLayerPoint(pos.x, pos.y)
                            log.debug("dragend sideboard piece #{@color}#{value} #{draggableImage.x()}/#{draggableImage.y()} at square: #{boardSquare}")
                        )

                    staticImage = new Kinetic.Image {
                        image: imgElement
                        draggable: false
                        x: x
                        y: y
                        width: width
                        height: height
                    }
                    number = if (@isUser) then @getInitialCount(value) else 0
                    numberText = new Kinetic.Text {
                        x: x+TEXT_OFFSET_X,
                        y: y+TEXT_OFFSET_Y,
                        text: number,
                        fontSize: 15,
                        fontFamily: 'Calibri',
                        fill: 'yellow'
                    }


                    square = {
                        rect: rect
                        numberText: numberText
                        staticImage: staticImage
                        draggableImage: draggableImage
                    }

                    @squares[value] = square
                    layer.add(square.rect)
                    layer.add(square.staticImage)
                    if (@isUser)
                        layer.add(square.draggableImage)
                    layer.add(square.numberText)
                    layer.draw()

        getImageUrl: (value) ->
            "#{ASSETS}images/stratego/#{@color}#{value}.png"
        getInitialCount: (value) ->
            switch (value)
              when StrategoPiece.BOMB then 6
              when StrategoPiece.FLAG then 1
              when StrategoPiece.SPY then 1
              when StrategoPiece.SCOUT then 6
              when StrategoPiece.MINER then 5
              when StrategoPiece.SERGENT then 5
              when StrategoPiece.LIEUTENANT then 4
              when StrategoPiece.CAPTAIN then 4
              when StrategoPiece.MAJOR then 3
              when StrategoPiece.COLONEL then 2
              when StrategoPiece.GENERAL then 1
              when StrategoPiece.MARSHAL then 1

    class RedSideboard extends StrategoSideboard
        constructor: (isUser, board) ->
            super('r', isUser, board)
        getStartPos: ->
            {
                x: START_X,
                y: RED_Y
            }


    class BlueSideboard extends StrategoSideboard
        constructor: (isUser, board) ->
            super('b', isUser, board)
        getStartPos: ->
            {
                x: START_X,
                y: BLUE_Y
            }

            

    {
        StrategoSideboard: StrategoSideboard
        RedSideboard: RedSideboard
        BlueSideboard: BlueSideboard
    }

])