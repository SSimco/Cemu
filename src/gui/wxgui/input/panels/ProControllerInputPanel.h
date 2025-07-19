#pragma once

#include <wx/gbsizer.h>
#include "input/emulated/ProController.h"
<<<<<<< HEAD:src/gui/input/panels/ProControllerInputPanel.h
#include "input/panels/InputPanel.h"
#include "components/wxInputDraw.h"
=======
#include "wxgui/input/panels/InputPanel.h"
#include "wxgui/components/wxInputDraw.h"
>>>>>>> public/main:src/gui/wxgui/input/panels/ProControllerInputPanel.h

class ProControllerInputPanel : public InputPanel
{
public:
	ProControllerInputPanel(wxWindow* parent);

	void on_timer(const EmulatedControllerPtr& emulated_controller, const ControllerPtr& controller) override;

private:
	wxInputDraw* m_left_draw, * m_right_draw;

	void add_button_row(wxGridBagSizer *sizer, sint32 row, sint32 column, const ProController::ButtonId &button_id);
};
