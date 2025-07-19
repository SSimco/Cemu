#pragma once

#include <wx/gbsizer.h>
<<<<<<< HEAD:src/gui/input/panels/VPADInputPanel.h
#include "input/panels/InputPanel.h"
=======
#include "wxgui/input/panels/InputPanel.h"
>>>>>>> public/main:src/gui/wxgui/input/panels/VPADInputPanel.h

class wxInputDraw;
class wxCheckBox;

class VPADInputPanel : public InputPanel
{
public:
	VPADInputPanel(wxWindow* parent);

	void on_timer(const EmulatedControllerPtr& emulated_controller, const ControllerPtr& controller) override;
	virtual void load_controller(const EmulatedControllerPtr& controller) override;

private:
	void OnVolumeChange(wxCommandEvent& event);

	wxInputDraw* m_left_draw, * m_right_draw;
	wxCheckBox* m_togglePadViewCheckBox;

	void add_button_row(wxGridBagSizer *sizer, sint32 row, sint32 column, const VPADController::ButtonId &button_id);
	void add_button_row(wxGridBagSizer *sizer, sint32 row, sint32 column, const VPADController::ButtonId &button_id, const wxString &label);
};
