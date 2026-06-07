package
{
    import flash.display.Sprite;
    import flash.text.TextField;
    import flash.text.TextFormat;
    import flash.events.MouseEvent;

    public class sample extends Sprite
    {
        private var button:Sprite;
        private var label:TextField;

        public function Main()
        {
            createButton();
        }

        private function createButton():void
        {
            // Create button shape
            button = new Sprite();
            button.graphics.beginFill(0x3399FF);
            button.graphics.drawRoundRect(0, 0, 200, 60, 10, 10);
            button.graphics.endFill();

            button.x = 100;
            button.y = 100;
            button.buttonMode = true;

            addChild(button);

            // Create button text
            label = new TextField();
            label.defaultTextFormat = new TextFormat("_sans", 20, 0xFFFFFF, true);
            label.width = 200;
            label.height = 40;
            label.text = "Click Me";
            label.selectable = false;
            label.y = 15;

            button.addChild(label);

            // Click event
            button.addEventListener(MouseEvent.CLICK, onButtonClick);
        }

        private function onButtonClick(event:MouseEvent):void
        {
            label.text = "Clicked!";
            trace("Button was clicked.");
        }
    }
}
