angular.module("app.stratego.functions", [])
    .factory('StrategoFunctions', ['$log', 
(log) ->

    SQUARE_X_OFFSET = 19
    SQUARE_Y_OFFSET = 115
    SQUARE_WIDTH = 40
    SQUARE_HEIGHT = 39

    squareToXy = (x,y) ->
        {
            x: SQUARE_X_OFFSET+((x-1)*SQUARE_WIDTH)
            y: SQUARE_Y_OFFSET+((10-y)*SQUARE_HEIGHT)
        } 

    {
        squareToXy : squareToXy
        SQUARE_X_OFFSET: SQUARE_X_OFFSET
        SQUARE_Y_OFFSET: SQUARE_Y_OFFSET
        SQUARE_WIDTH : SQUARE_WIDTH
        SQUARE_HEIGHT : SQUARE_HEIGHT
    }


])